package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class BuildingService {

    private static final Set<String> STARTER_BUILDINGS = Set.of("metal_mine", "crystal_mine", "solar_plant");

    private final PlanetBuildingRepository buildingRepository;
    private final ConstructionJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;

    public BuildingService(PlanetBuildingRepository buildingRepository, ConstructionJobRepository jobRepository,
                            CatalogService catalogService, ResourceService resourceService) {
        this.buildingRepository = buildingRepository;
        this.jobRepository = jobRepository;
        this.catalogService = catalogService;
        this.resourceService = resourceService;
    }

    @Transactional
    public void initializeStarter(Long planetId) {
        for (String key : STARTER_BUILDINGS) {
            buildingRepository.save(new PlanetBuilding(planetId, key, 1));
        }
    }

    public List<BuildingView> listForPlanet(Long planetId) {
        var activeJob = jobRepository.findByPlanetId(planetId);

        return catalogService.buildings().stream()
                .map(definition -> {
                    int currentLevel = currentLevel(planetId, definition.key());
                    int targetLevel = currentLevel + 1;
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getBuildingKey().equals(definition.key());
                    return new BuildingView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            currentLevel,
                            catalogService.costFor(definition, targetLevel),
                            catalogService.buildTimeFor(definition, targetLevel).toSeconds(),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null
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
        int targetLevel = currentLevel(planetId, buildingKey) + 1;

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

    private int currentLevel(Long planetId, String buildingKey) {
        return buildingRepository.findByPlanetIdAndBuildingKey(planetId, buildingKey)
                .map(PlanetBuilding::getLevel)
                .orElse(0);
    }
}
