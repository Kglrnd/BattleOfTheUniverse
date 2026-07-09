package de.kugi.dev.battleoftheuniverse.research;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.TechnologyDefinition;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.dto.DriveOption;
import de.kugi.dev.battleoftheuniverse.research.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchStartResponse;
import de.kugi.dev.battleoftheuniverse.research.dto.TechnologyView;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ResearchService {

    private final TechnologyRepository technologyRepository;
    private final ResearchJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final BuildingService buildingService;

    public ResearchService(TechnologyRepository technologyRepository, ResearchJobRepository jobRepository,
                            CatalogService catalogService, ResourceService resourceService,
                            PlanetService planetService, BuildingService buildingService) {
        this.technologyRepository = technologyRepository;
        this.jobRepository = jobRepository;
        this.catalogService = catalogService;
        this.resourceService = resourceService;
        this.planetService = planetService;
        this.buildingService = buildingService;
    }

    public List<TechnologyView> listForUser(Long userId) {
        var activeJob = jobRepository.findByUserId(userId);

        return catalogService.technologies().stream()
                .map(definition -> {
                    int level = currentLevel(userId, definition.key());
                    int targetLevel = level + 1;
                    boolean isBeingResearched = activeJob.isPresent()
                            && activeJob.get().getTechnologyKey().equals(definition.key());
                    List<LockedRequirement> missingRequirements = missingRequirements(userId, definition.requirements());
                    return new TechnologyView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            definition.driveScope(),
                            level,
                            catalogService.costFor(definition, targetLevel),
                            catalogService.researchTimeFor(definition, targetLevel).toSeconds(),
                            isBeingResearched,
                            isBeingResearched ? activeJob.get().getTargetLevel() : null,
                            isBeingResearched ? activeJob.get().getEndsAt() : null,
                            missingRequirements.isEmpty(),
                            missingRequirements
                    );
                })
                .toList();
    }

    @Transactional
    public ResearchStartResponse startResearch(Long userId, Long planetId, String technologyKey) {
        if (jobRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A research is already in progress for this account");
        }

        TechnologyDefinition definition = catalogService.technology(technologyKey);
        if (!missingRequirements(userId, definition.requirements()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requirements not met for technology: " + technologyKey);
        }
        int targetLevel = currentLevel(userId, technologyKey) + 1;

        ResourceCost cost = catalogService.costFor(definition, targetLevel);
        resourceService.debit(planetId, cost);

        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plus(catalogService.researchTimeFor(definition, targetLevel));
        jobRepository.save(new ResearchJob(userId, planetId, technologyKey, targetLevel, startedAt, endsAt));

        return new ResearchStartResponse(technologyKey, targetLevel, endsAt);
    }

    @Transactional
    public void completeDueJobs() {
        for (ResearchJob job : jobRepository.findByEndsAtBefore(Instant.now())) {
            Technology technology = technologyRepository
                    .findByUserIdAndTechnologyKey(job.getUserId(), job.getTechnologyKey())
                    .orElseGet(() -> new Technology(job.getUserId(), job.getTechnologyKey(), 0));
            technology.setLevel(job.getTargetLevel());
            technologyRepository.save(technology);
            jobRepository.delete(job);
        }
    }

    /** Dev-only convenience: sets every catalog technology (including all drives) to the given level. */
    @Transactional
    public void maxAllTechnologies(Long userId, int level) {
        for (TechnologyDefinition definition : catalogService.technologies()) {
            Technology technology = technologyRepository.findByUserIdAndTechnologyKey(userId, definition.key())
                    .orElseGet(() -> new Technology(userId, definition.key(), 0));
            technology.setLevel(level);
            technologyRepository.save(technology);
        }
    }

    /**
     * The travel-speed multiplier a specific researched drive grants, or empty if the user
     * hasn't researched it or its scope doesn't reach far enough. A drive's scope also covers
     * every narrower one, so e.g. a researched GALAXY drive answers for a same-system hop
     * too — see {@link DriveScope}.
     */
    public Optional<Double> speedMultiplierForDrive(Long userId, String driveKey, DriveScope requiredScope) {
        int level = currentLevel(userId, driveKey);
        if (level <= 0) {
            return Optional.empty();
        }
        TechnologyDefinition definition = catalogService.technology(driveKey);
        if (definition.driveScope() == DriveScope.NONE || definition.driveScope().ordinal() < requiredScope.ordinal()) {
            return Optional.empty();
        }
        return Optional.of(definition.baseSpeedMultiplier() + definition.driveSpeedBonusPerLevel() * level);
    }

    /**
     * Every researched drive the user could pick for a mission of the given range, each with
     * its own current speed multiplier - lets the player compare and choose rather than the
     * game silently picking the fastest one for them.
     */
    public List<DriveOption> listAvailableDrives(Long userId, DriveScope requiredScope) {
        List<DriveOption> options = new ArrayList<>();
        for (Technology technology : technologyRepository.findByUserId(userId)) {
            if (technology.getLevel() <= 0) {
                continue;
            }
            TechnologyDefinition definition = catalogService.technology(technology.getTechnologyKey());
            if (definition.driveScope() == DriveScope.NONE || definition.driveScope().ordinal() < requiredScope.ordinal()) {
                continue;
            }
            double multiplier = definition.baseSpeedMultiplier() + definition.driveSpeedBonusPerLevel() * technology.getLevel();
            options.add(new DriveOption(definition.key(), definition.name(), definition.driveScope(), technology.getLevel(), multiplier));
        }
        return options;
    }

    private int currentLevel(Long userId, String technologyKey) {
        return technologyRepository.findByUserIdAndTechnologyKey(userId, technologyKey)
                .map(Technology::getLevel)
                .orElse(0);
    }

    private List<LockedRequirement> missingRequirements(Long userId, List<Requirement> requirements) {
        List<LockedRequirement> missing = new ArrayList<>();
        for (Requirement requirement : requirements) {
            int currentLevel = switch (requirement.type()) {
                case TECHNOLOGY -> currentLevel(userId, requirement.key());
                case BUILDING -> highestBuildingLevel(userId, requirement.key());
            };
            if (currentLevel < requirement.level()) {
                String label = requirement.type() == RequirementType.TECHNOLOGY
                        ? catalogService.technology(requirement.key()).name()
                        : catalogService.building(requirement.key()).name();
                missing.add(new LockedRequirement(label, requirement.level(), currentLevel));
            }
        }
        return missing;
    }

    /** Research is account-wide, so a building requirement is met if any owned planet qualifies. */
    private int highestBuildingLevel(Long userId, String buildingKey) {
        return planetService.listMine(userId).stream()
                .mapToInt(planet -> buildingService.levelOf(planet.getId(), buildingKey))
                .max()
                .orElse(0);
    }
}
