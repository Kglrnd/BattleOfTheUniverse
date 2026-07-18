package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.building.dto.ProductionLevelView;
import de.kugi.dev.battleoftheuniverse.building.dto.ResourceProductionView;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementChecker;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.planet.EfficiencyRoll;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private static final String MAIN_BUILDING_KEY = "main_building";
    private static final Set<String> STARTER_BUILDINGS = Set.of(MAIN_BUILDING_KEY);
    private static final String RESEARCH_LAB_KEY = "research_lab";

    /** Matches the "construction_hub" catalog entry. */
    private static final String CONSTRUCTION_HUB_KEY = "construction_hub";
    private static final double BUILD_TIME_REDUCTION_PER_HUB_LEVEL = 0.015;
    /** However high the hub's level, a building's time is never cut by more than 90%. */
    private static final double MIN_BUILD_TIME_MULTIPLIER = 0.1;

    private final PlanetBuildingRepository buildingRepository;
    private final ConstructionJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void initializeStarter(Long planetId) {
        for (String key : STARTER_BUILDINGS) {
            buildingRepository.save(new PlanetBuilding(planetId, key, 1));
        }
        initializeProducingBuildings(planetId);
    }

    /**
     * Creates a level-0 row (with a freshly rolled production efficiency) for every catalog
     * resource-producing building that doesn't already have one on this planet - idempotent,
     * so it's safe to call both synchronously right after colonization (for the "colony founded"
     * message) and from the async {@code PlanetCreated} listener that bootstraps every new planet;
     * whichever runs first wins and the other is a no-op. Returns every producing building's
     * current efficiency, keyed by catalog key.
     */
    @Transactional
    public Map<String, Double> initializeProducingBuildings(Long planetId) {
        Map<String, Double> efficiencies = new LinkedHashMap<>();
        for (BuildingDefinition definition : catalogService.buildings()) {
            if (definition.producesResource() == ResourceKey.NONE) {
                continue;
            }
            PlanetBuilding building = buildingRepository.findByPlanetIdAndBuildingKey(planetId, definition.key())
                    .orElseGet(() -> buildingRepository.save(newPlanetBuilding(planetId, definition.key(), 0)));
            efficiencies.put(definition.key(), building.getProductionEfficiency());
        }
        return efficiencies;
    }

    /** Dev-only convenience: sets every catalog building on a planet to the given level. */
    @Transactional
    public void maxAllBuildings(Long planetId, int level) {
        for (BuildingDefinition definition : catalogService.buildings()) {
            PlanetBuilding building = buildingRepository.findByPlanetIdAndBuildingKey(planetId, definition.key())
                    .orElseGet(() -> newPlanetBuilding(planetId, definition.key(), 0));
            building.setLevel(level);
            buildingRepository.save(building);
        }
    }

    /** Rolls a fresh production efficiency for resource-producing buildings; non-producers keep the 100% default. */
    private PlanetBuilding newPlanetBuilding(Long planetId, String buildingKey, int level) {
        PlanetBuilding building = new PlanetBuilding(planetId, buildingKey, level);
        if (catalogService.building(buildingKey).producesResource() != ResourceKey.NONE) {
            building.setProductionEfficiency(rollProductionEfficiency());
        }
        return building;
    }

    /** Uniform roll in [85.00, 109.99], fixed for the building's lifetime - mirrors PlanetService.rollResearchEfficiency(). */
    private double rollProductionEfficiency() {
        return EfficiencyRoll.roll(random);
    }

    /**
     * The catalog's base build time for this building/level, sped up by the planet's Construction
     * Hub level - 1.5% per level, clamped so it can never be cut by more than
     * {@value #MIN_BUILD_TIME_MULTIPLIER}'s complement. The Construction Hub speeds up every
     * <i>other</i> building's construction, not its own - upgrading the hub itself always takes
     * the plain catalog time.
     */
    private Duration adjustedBuildTime(Long planetId, BuildingDefinition definition, int targetLevel) {
        Duration base = catalogService.buildTimeFor(definition, targetLevel);
        if (definition.key().equals(CONSTRUCTION_HUB_KEY)) {
            return base;
        }
        int hubLevel = levelOf(planetId, CONSTRUCTION_HUB_KEY);
        if (hubLevel <= 0) {
            return base;
        }
        double multiplier = Math.max(MIN_BUILD_TIME_MULTIPLIER, 1.0 - BUILD_TIME_REDUCTION_PER_HUB_LEVEL * hubLevel);
        return Duration.ofSeconds(Math.round(base.toSeconds() * multiplier));
    }

    public List<BuildingView> listForPlanet(Long planetId) {
        var activeJob = jobRepository.findByPlanetId(planetId);
        Planet planet = planetService.getById(planetId);
        Map<String, PlanetBuilding> byKey = buildingRepository.findByPlanetId(planetId).stream()
                .collect(Collectors.toMap(PlanetBuilding::getBuildingKey, Function.identity()));

        return catalogService.buildings().stream()
                .map(definition -> {
                    PlanetBuilding existing = byKey.get(definition.key());
                    int currentLevel = existing != null ? existing.getLevel() : 0;
                    int targetLevel = currentLevel + 1;
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getBuildingKey().equals(definition.key());
                    List<LockedRequirement> missingRequirements = missingRequirements(planetId, definition.requirements());
                    boolean isResearchLab = definition.key().equals(RESEARCH_LAB_KEY);
                    boolean isConstructionHub = definition.key().equals(CONSTRUCTION_HUB_KEY);
                    boolean producesResource = definition.producesResource() != ResourceKey.NONE;
                    return new BuildingView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            currentLevel,
                            catalogService.costFor(definition, targetLevel),
                            adjustedBuildTime(planetId, definition, targetLevel).toSeconds(),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null,
                            missingRequirements.isEmpty(),
                            missingRequirements,
                            isResearchLab ? planet.getResearchEfficiency() : null,
                            producesResource && existing != null ? existing.getProductionEfficiency() : null,
                            isConstructionHub && currentLevel > 0 ? currentLevel * BUILD_TIME_REDUCTION_PER_HUB_LEVEL * 100.0 : null
                    );
                })
                .toList();
    }

    /**
     * Current production/hour for every resource-producing building on the planet, plus up to 5
     * levels below (clipped at 0) and 5 levels above (no upper level cap exists, so this window
     * is always full) the current level - lets the resources page show both a history and a
     * preview of what upgrading further is worth.
     */
    public List<ResourceProductionView> productionOverview(Long planetId) {
        Map<String, PlanetBuilding> byKey = buildingRepository.findByPlanetId(planetId).stream()
                .collect(Collectors.toMap(PlanetBuilding::getBuildingKey, Function.identity()));

        return catalogService.buildings().stream()
                .filter(definition -> definition.producesResource() != ResourceKey.NONE)
                .map(definition -> {
                    PlanetBuilding existing = byKey.get(definition.key());
                    int currentLevel = existing != null ? existing.getLevel() : 0;
                    double efficiencyFraction = (existing != null ? existing.getProductionEfficiency() : 100.0) / 100.0;

                    int fromLevel = Math.max(0, currentLevel - 5);
                    int toLevel = currentLevel + 5;
                    List<ProductionLevelView> levels = new ArrayList<>();
                    for (int level = fromLevel; level <= toLevel; level++) {
                        double perHour = catalogService.productionPerHour(definition, level) * efficiencyFraction;
                        levels.add(new ProductionLevelView(level, perHour, level == currentLevel));
                    }

                    double currentPerHour = catalogService.productionPerHour(definition, currentLevel) * efficiencyFraction;
                    return new ResourceProductionView(definition.key(), definition.name(), definition.description(),
                            definition.producesResource(), definition.producesResource().getDisplayName(),
                            currentLevel, efficiencyFraction * 100.0, currentPerHour, levels);
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
        Instant endsAt = startedAt.plus(adjustedBuildTime(planetId, definition, targetLevel));
        try {
            jobRepository.save(new ConstructionJob(planetId, buildingKey, targetLevel, startedAt, endsAt));
        } catch (DataIntegrityViolationException e) {
            // The upfront isPresent() check passed, but a concurrent upgrade request beat us to
            // the insert - the unique constraint on construction_jobs(planet_id) is the actual
            // backstop; fail the same way the check would, rolling back the debit above too.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A construction is already in progress on this planet");
        }

        return new UpgradeResponse(buildingKey, targetLevel, endsAt);
    }

    @Transactional
    public void completeDueJobs() {
        for (ConstructionJob job : jobRepository.findByEndsAtBefore(Instant.now())) {
            PlanetBuilding building = buildingRepository
                    .findByPlanetIdAndBuildingKey(job.getPlanetId(), job.getBuildingKey())
                    .orElseGet(() -> newPlanetBuilding(job.getPlanetId(), job.getBuildingKey(), 0));
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

    /** Buildings are never technology-gated - a TECHNOLOGY requirement (if one ever appears) never blocks. */
    private List<LockedRequirement> missingRequirements(Long planetId, List<Requirement> requirements) {
        return RequirementChecker.unmet(catalogService, requirements, (type, key) ->
                        type == RequirementType.BUILDING ? levelOf(planetId, key) : Integer.MAX_VALUE)
                .stream()
                .map(gap -> new LockedRequirement(gap.label(), gap.requiredLevel(), gap.currentLevel()))
                .toList();
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
            if (buildingRepository.findByPlanetIdAndBuildingKey(planetId, MAIN_BUILDING_KEY).isEmpty()) {
                buildingRepository.save(new PlanetBuilding(planetId, MAIN_BUILDING_KEY, 1));
            }
        }
    }
}
