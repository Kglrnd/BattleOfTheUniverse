package de.kugi.dev.battleoftheuniverse.defense;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DefenseDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementChecker;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.defense.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.defense.dto.TowerBuildResponse;
import de.kugi.dev.battleoftheuniverse.defense.dto.TowerView;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefenseService {

    private final TowerRepository towerRepository;
    private final DefenseJobRepository jobRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final BuildingService buildingService;

    public List<TowerView> listForPlanet(Long planetId) {
        var activeJob = jobRepository.findByPlanetId(planetId);
        Map<String, Integer> ownedByKey = towerRepository.findByPlanetId(planetId).stream()
                .collect(Collectors.toMap(Tower::getTowerKey, Tower::getQuantity));

        return catalogService.defenses().stream()
                .map(definition -> {
                    int owned = ownedByKey.getOrDefault(definition.key(), 0);
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getTowerKey().equals(definition.key());
                    List<LockedRequirement> missingRequirements = missingRequirements(planetId, definition.requirements());
                    return new TowerView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            owned,
                            definition.defense(),
                            definition.baseCost(),
                            definition.baseBuildTimeSeconds(),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getQuantity() : null,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null,
                            missingRequirements.isEmpty(),
                            missingRequirements
                    );
                })
                .toList();
    }

    @Transactional
    public TowerBuildResponse queueTower(Long planetId, String towerKey, int quantity) {
        if (jobRepository.findByPlanetId(planetId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A defense order is already in progress on this planet");
        }

        DefenseDefinition definition = catalogService.defense(towerKey);
        if (!missingRequirements(planetId, definition.requirements()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requirements not met for defense: " + towerKey);
        }

        ResourceCost cost = definition.baseCost().scaled(quantity);
        resourceService.debit(planetId, cost);

        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plusSeconds((long) definition.baseBuildTimeSeconds() * quantity);
        try {
            jobRepository.save(new DefenseJob(planetId, towerKey, quantity, startedAt, endsAt));
        } catch (DataIntegrityViolationException e) {
            // The upfront isPresent() check passed, but a concurrent build request beat us to
            // the insert - the unique constraint on defense_jobs(planet_id) is the actual
            // backstop; fail the same way the check would, rolling back the debit above too.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A defense order is already in progress on this planet");
        }

        return new TowerBuildResponse(towerKey, quantity, endsAt);
    }

    @Transactional
    public void completeDueJobs() {
        for (DefenseJob job : jobRepository.findByEndsAtBefore(Instant.now())) {
            Tower tower = towerRepository.findByPlanetIdAndTowerKey(job.getPlanetId(), job.getTowerKey())
                    .orElseGet(() -> new Tower(job.getPlanetId(), job.getTowerKey(), 0));
            tower.setQuantity(tower.getQuantity() + job.getQuantity());
            towerRepository.save(tower);
            jobRepository.delete(job);
        }
    }

    /** Every tower stationed on a planet, keyed by defense catalog key. Used by combat to read defensive strength. */
    public Map<String, Integer> stationedTowers(Long planetId) {
        return towerRepository.findByPlanetId(planetId).stream()
                .collect(Collectors.toMap(Tower::getTowerKey, Tower::getQuantity));
    }

    /** Reduces stationed tower quantities by the given amounts. Used by combat to apply losses. */
    @Transactional
    public void applyLosses(Long planetId, Map<String, Integer> losses) {
        losses.forEach((towerKey, lost) -> {
            if (lost <= 0) {
                return;
            }
            towerRepository.findByPlanetIdAndTowerKey(planetId, towerKey).ifPresent(tower -> {
                int remaining = Math.max(0, tower.getQuantity() - lost);
                if (remaining == 0) {
                    towerRepository.delete(tower);
                } else {
                    tower.setQuantity(remaining);
                    towerRepository.save(tower);
                }
            });
        });
    }

    /** Admin-triggered game reset: clears every tower and in-progress defense order, game-wide. */
    @Transactional
    public void wipeAll() {
        jobRepository.deleteAll();
        towerRepository.deleteAll();
    }

    /** Clears a single planet's towers and any in-progress defense order - used when a planet is destroyed. */
    @Transactional
    public void deleteAllForPlanet(Long planetId) {
        jobRepository.findByPlanetId(planetId).ifPresent(jobRepository::delete);
        towerRepository.deleteAll(towerRepository.findByPlanetId(planetId));
    }

    /** Defense towers are never technology-gated - a TECHNOLOGY requirement (if one ever appears) never blocks. */
    private List<LockedRequirement> missingRequirements(Long planetId, List<Requirement> requirements) {
        return RequirementChecker.unmet(catalogService, requirements, (type, key) ->
                        type == RequirementType.BUILDING ? buildingService.levelOf(planetId, key) : Integer.MAX_VALUE)
                .stream()
                .map(gap -> new LockedRequirement(gap.label(), gap.requiredLevel(), gap.currentLevel()))
                .toList();
    }
}
