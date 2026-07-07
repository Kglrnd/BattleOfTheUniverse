package de.kugi.dev.battleoftheuniverse.research;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.TechnologyDefinition;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchStartResponse;
import de.kugi.dev.battleoftheuniverse.research.dto.TechnologyView;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ResearchService {

    private final TechnologyRepository technologyRepository;
    private final ResearchJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;

    public ResearchService(TechnologyRepository technologyRepository, ResearchJobRepository jobRepository,
                            CatalogService catalogService, ResourceService resourceService) {
        this.technologyRepository = technologyRepository;
        this.jobRepository = jobRepository;
        this.catalogService = catalogService;
        this.resourceService = resourceService;
    }

    public List<TechnologyView> listForUser(Long userId) {
        var activeJob = jobRepository.findByUserId(userId);

        return catalogService.technologies().stream()
                .map(definition -> {
                    int level = currentLevel(userId, definition.key());
                    int targetLevel = level + 1;
                    boolean isBeingResearched = activeJob.isPresent()
                            && activeJob.get().getTechnologyKey().equals(definition.key());
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
                            isBeingResearched ? activeJob.get().getEndsAt() : null
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

    /**
     * The best travel-speed multiplier the user's researched drives grant for a mission
     * of the given range, or empty if no researched drive reaches that far. A drive's
     * scope also covers every narrower one, so e.g. a researched GALAXY drive answers
     * for a same-system hop too — see {@link DriveScope}.
     */
    public Optional<Double> speedMultiplierFor(Long userId, DriveScope requiredScope) {
        double best = -1;
        for (Technology technology : technologyRepository.findByUserId(userId)) {
            if (technology.getLevel() <= 0) {
                continue;
            }
            TechnologyDefinition definition = catalogService.technology(technology.getTechnologyKey());
            if (definition.driveScope() == DriveScope.NONE || definition.driveScope().ordinal() < requiredScope.ordinal()) {
                continue;
            }
            double multiplier = 1.0 + definition.driveSpeedBonusPerLevel() * technology.getLevel();
            best = Math.max(best, multiplier);
        }
        return best < 0 ? Optional.empty() : Optional.of(best);
    }

    private int currentLevel(Long userId, String technologyKey) {
        return technologyRepository.findByUserIdAndTechnologyKey(userId, technologyKey)
                .map(Technology::getLevel)
                .orElse(0);
    }
}
