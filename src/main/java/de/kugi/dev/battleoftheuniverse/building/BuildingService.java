package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private static final Set<String> STARTER_BUILDINGS = Set.of("main_building");
    private static final String RESEARCH_LAB_KEY = "research_lab";

    private final PlanetBuildingRepository buildingRepository;
    private final ConstructionJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;

    @Transactional
    public void initializeStarter(Long planetId) {
        for (String key : STARTER_BUILDINGS) {
            buildingRepository.save(new PlanetBuilding(planetId, key, 1));
        }
    }

    /** Dev-only convenience: sets every catalog building on a planet to the given level. */
    @Transactional
    public void maxAllBuildings(Long planetId, int level) {
        for (BuildingDefinition definition : catalogService.buildings()) {
            PlanetBuilding building = buildingRepository.findByPlanetIdAndBuildingKey(planetId, definition.key())
                    .orElseGet(() -> new PlanetBuilding(planetId, definition.key(), 0));
            building.setLevel(level);
            buildingRepository.save(building);
        }
    }

    public List<BuildingView> listForPlanet(Long planetId) {
        var activeJob = jobRepository.findByPlanetId(planetId);
        Planet planet = planetService.getById(planetId);

        return catalogService.buildings().stream()
                .map(definition -> {
                    int currentLevel = levelOf(planetId, definition.key());
                    int targetLevel = currentLevel + 1;
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getBuildingKey().equals(definition.key());
                    List<LockedRequirement> missingRequirements = missingRequirements(planetId, definition.requirements());
                    boolean isResearchLab = definition.key().equals(RESEARCH_LAB_KEY);
                    return new BuildingView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            currentLevel,
                            catalogService.costFor(definition, targetLevel),
                            catalogService.buildTimeFor(definition, targetLevel).toSeconds(),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null,
                            missingRequirements.isEmpty(),
                            missingRequirements,
                            isResearchLab ? planet.getResearchEfficiency() : null
                    );
                })
                .toList();
    }

    @Transactional
    public UpgradeResponse upgrade(Long planetId, String buildingKey) {
        if (jobRepository.findByPlanetId(planetId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A construction is already in progress on this planet");
        }

        BuildingDefinition definition = catalogService.building(buildingKey);
        if (!requirementsMet(planetId, definition.requirements())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requirements not met for building: " + buildingKey);
        }
        int targetLevel = levelOf(planetId, buildingKey) + 1;

        ResourceCost cost = catalogService.costFor(definition, targetLevel);
        resourceService.debit(planetId, cost);

        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plus(catalogService.buildTimeFor(definition, targetLevel));
        jobRepository.save(new ConstructionJob(planetId, buildingKey, targetLevel, startedAt, endsAt));

        return new UpgradeResponse(buildingKey, targetLevel, endsAt);
    }

    @Transactional
    public void completeDueJobs() {
        for (ConstructionJob job : jobRepository.findByEndsAtBefore(Instant.now())) {
            PlanetBuilding building = buildingRepository
                    .findByPlanetIdAndBuildingKey(job.getPlanetId(), job.getBuildingKey())
                    .orElseGet(() -> new PlanetBuilding(job.getPlanetId(), job.getBuildingKey(), 0));
            building.setLevel(job.getTargetLevel());
            buildingRepository.save(building);
            jobRepository.delete(job);
        }
    }

    /** Current level of a building on a planet, or 0 if it hasn't been built yet. */
    public int levelOf(Long planetId, String buildingKey) {
        return buildingRepository.findByPlanetIdAndBuildingKey(planetId, buildingKey)
                .map(PlanetBuilding::getLevel)
                .orElse(0);
    }

    private boolean requirementsMet(Long planetId, List<Requirement> requirements) {
        return missingRequirements(planetId, requirements).isEmpty();
    }

    private List<LockedRequirement> missingRequirements(Long planetId, List<Requirement> requirements) {
        List<LockedRequirement> missing = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (requirement.type() != RequirementType.BUILDING) {
                continue;
            }
            int currentLevel = levelOf(planetId, requirement.key());
            if (currentLevel < requirement.level()) {
                missing.add(new LockedRequirement(catalogService.building(requirement.key()).name(), requirement.level(), currentLevel));
            }
        }
        return missing;
    }

    /** Admin-triggered game reset: clears every building and in-progress construction, game-wide. */
    @Transactional
    public void wipeAll() {
        jobRepository.deleteAll();
        buildingRepository.deleteAll();
    }

    /** Clears a single planet's buildings and any in-progress construction - used when a planet is destroyed. */
    @Transactional
    public void deleteAllForPlanet(Long planetId) {
        jobRepository.findByPlanetId(planetId).ifPresent(jobRepository::delete);
        buildingRepository.deleteAll(buildingRepository.findByPlanetId(planetId));
    }

    /** Backfills a level-1 command center on any planet that predates the main-building requirement system. */
    @Transactional
    public void backfillMainBuilding() {
        for (Long planetId : planetService.listAllIds()) {
            if (buildingRepository.findByPlanetIdAndBuildingKey(planetId, "main_building").isEmpty()) {
                buildingRepository.save(new PlanetBuilding(planetId, "main_building", 1));
            }
        }
    }
}
