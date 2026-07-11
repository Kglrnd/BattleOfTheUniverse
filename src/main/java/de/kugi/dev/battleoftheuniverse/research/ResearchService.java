package de.kugi.dev.battleoftheuniverse.research;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.TechnologyDefinition;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.dto.DriveOption;
import de.kugi.dev.battleoftheuniverse.research.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchPlanetOption;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchStartResponse;
import de.kugi.dev.battleoftheuniverse.research.dto.TechnologyView;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResearchService {

    private static final String RESEARCH_LAB_KEY = "research_lab";

    private final TechnologyRepository technologyRepository;
    private final ResearchJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final BuildingService buildingService;

    public List<TechnologyView> listForUser(Long userId) {
        var activeJob = jobRepository.findByUserId(userId);
        Optional<Planet> activePlanet = planetService.findActiveResearchPlanet(userId);

        return catalogService.technologies().stream()
                .map(definition -> {
                    int level = levelOf(userId, definition.key());
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
                            adjustedResearchTime(definition, targetLevel, activePlanet).toSeconds(),
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
    public ResearchStartResponse startResearch(Long userId, String technologyKey) {
        if (jobRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A research is already in progress for this account");
        }
        Planet activePlanet = planetService.findActiveResearchPlanet(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active research planet selected"));

        TechnologyDefinition definition = catalogService.technology(technologyKey);
        if (!missingRequirements(userId, definition.requirements()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requirements not met for technology: " + technologyKey);
        }
        int targetLevel = levelOf(userId, technologyKey) + 1;

        ResourceCost cost = catalogService.costFor(definition, targetLevel);
        resourceService.debit(activePlanet.getId(), cost);

        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plus(adjustedResearchTime(definition, targetLevel, Optional.of(activePlanet)));
        jobRepository.save(new ResearchJob(userId, activePlanet.getId(), technologyKey, targetLevel, startedAt, endsAt));

        return new ResearchStartResponse(technologyKey, targetLevel, endsAt);
    }

    /** Every owned planet as a candidate research planet, with its research-lab level and suitability. */
    public List<ResearchPlanetOption> listResearchPlanetOptions(Long userId) {
        Optional<Planet> activePlanet = planetService.findActiveResearchPlanet(userId);
        return planetService.listMine(userId).stream()
                .map(planet -> new ResearchPlanetOption(
                        planet.getId(),
                        planet.getName(),
                        planet.getCoordinates(),
                        planet.getResearchEfficiency(),
                        buildingService.levelOf(planet.getId(), RESEARCH_LAB_KEY),
                        activePlanet.isPresent() && activePlanet.get().getId().equals(planet.getId())
                ))
                .toList();
    }

    @Transactional
    public ResearchPlanetOption activateResearchPlanet(Long userId, Long planetId) {
        if (buildingService.levelOf(planetId, RESEARCH_LAB_KEY) < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This planet needs a Research Lab before it can host research");
        }
        Planet planet = planetService.activateResearchPlanet(userId, planetId);
        return new ResearchPlanetOption(planet.getId(), planet.getName(), planet.getCoordinates(),
                planet.getResearchEfficiency(), buildingService.levelOf(planet.getId(), RESEARCH_LAB_KEY), true);
    }

    /** Base research time scaled by the active research planet's suitability (100% = normal, see Planet.researchEfficiency). */
    private Duration adjustedResearchTime(TechnologyDefinition definition, int targetLevel, Optional<Planet> activePlanet) {
        Duration base = catalogService.researchTimeFor(definition, targetLevel);
        if (activePlanet.isEmpty()) {
            return base;
        }
        double multiplier = 2.0 - activePlanet.get().getResearchEfficiency() / 100.0;
        return Duration.ofSeconds(Math.round(base.toSeconds() * multiplier));
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

    /** Admin-triggered game reset: clears every researched technology and in-progress research, game-wide. */
    @Transactional
    public void wipeAll() {
        jobRepository.deleteAll();
        technologyRepository.deleteAll();
    }

    /**
     * The travel-speed multiplier a specific researched drive grants, or empty if the user
     * hasn't researched it or its scope is too wide for the mission (overkill drives aren't
     * offered for a narrower trip). A narrower drive works for a wider trip too, just very
     * slowly, since travel time is driven by raw distance, not by scope — see {@link DriveScope}.
     */
    public Optional<Double> speedMultiplierForDrive(Long userId, String driveKey, DriveScope requiredScope) {
        int level = levelOf(userId, driveKey);
        if (level <= 0) {
            return Optional.empty();
        }
        TechnologyDefinition definition = catalogService.technology(driveKey);
        if (definition.driveScope() == DriveScope.NONE || definition.driveScope().ordinal() > requiredScope.ordinal()) {
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
            if (definition.driveScope() == DriveScope.NONE || definition.driveScope().ordinal() > requiredScope.ordinal()) {
                continue;
            }
            double multiplier = definition.baseSpeedMultiplier() + definition.driveSpeedBonusPerLevel() * technology.getLevel();
            options.add(new DriveOption(definition.key(), definition.name(), definition.driveScope(), technology.getLevel(), multiplier));
        }
        return options;
    }

    public int levelOf(Long userId, String technologyKey) {
        return technologyRepository.findByUserIdAndTechnologyKey(userId, technologyKey)
                .map(Technology::getLevel)
                .orElse(0);
    }

    private List<LockedRequirement> missingRequirements(Long userId, List<Requirement> requirements) {
        List<LockedRequirement> missing = new ArrayList<>();
        for (Requirement requirement : requirements) {
            int currentLevel = switch (requirement.type()) {
                case TECHNOLOGY -> levelOf(userId, requirement.key());
                case BUILDING -> activeResearchPlanetBuildingLevel(userId, requirement.key());
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

    /** Research only happens from the account's active research planet - 0 if none is set. */
    private int activeResearchPlanetBuildingLevel(Long userId, String buildingKey) {
        return planetService.findActiveResearchPlanet(userId)
                .map(planet -> buildingService.levelOf(planet.getId(), buildingKey))
                .orElse(0);
    }
}
