package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The resource ledger for a planet. Deliberately knows nothing about buildings —
 * {@code building} is the one that knows production formulas and calls into this
 * service to credit/debit amounts, which keeps the dependency one-directional.
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final PlanetResourceRepository repository;

    @Transactional
    public void initializeStarter(Long planetId) {
        repository.save(new PlanetResource(planetId, ResourceKey.METAL, 500));
        repository.save(new PlanetResource(planetId, ResourceKey.CRYSTAL, 300));
        repository.save(new PlanetResource(planetId, ResourceKey.DEUTERIUM, 100));
        repository.save(new PlanetResource(planetId, ResourceKey.HYDROGEN, 50));
        repository.save(new PlanetResource(planetId, ResourceKey.ENERGY, 0));
    }

    public List<PlanetResource> raw(Long planetId) {
        return repository.findByPlanetId(planetId);
    }

    /** Admin-triggered game reset: clears every resource ledger, game-wide. */
    @Transactional
    public void wipeAll() {
        repository.deleteAll();
    }

    /** Clears a single planet's resource ledger - used when a planet is destroyed. */
    @Transactional
    public void deleteAllForPlanet(Long planetId) {
        repository.deleteAll(repository.findByPlanetId(planetId));
    }

    /**
     * Credits whatever whole units of production have accrued since the resource's
     * {@code lastUpdate}, then advances {@code lastUpdate} only by the time that was
     * actually "spent" producing those whole units — the fractional remainder carries
     * over to the next call instead of being lost, regardless of how often this runs.
     */
    @Transactional
    public void applyProduction(Long planetId, ResourceKey key, double hourlyRate) {
        applyOneProduction(planetId, key, hourlyRate);
    }

    /**
     * Same as {@link #applyProduction(Long, ResourceKey, double)}, but applies every
     * resource a single planet produces in one transaction instead of one transaction per
     * resource - {@code ProductionScheduler} groups the whole game's producing buildings by
     * planet before calling this, so a planet producing e.g. metal, crystal and deuterium
     * costs one transaction per tick instead of three.
     */
    @Transactional
    public void applyProduction(Long planetId, Map<ResourceKey, Double> hourlyRatesByResource) {
        hourlyRatesByResource.forEach((key, hourlyRate) -> applyOneProduction(planetId, key, hourlyRate));
    }

    private void applyOneProduction(Long planetId, ResourceKey key, double hourlyRate) {
        if (hourlyRate <= 0) {
            return;
        }
        PlanetResource resource = require(planetId, key);
        Instant lastUpdate = resource.getLastUpdate();
        Instant now = Instant.now();
        double elapsedHours = Duration.between(lastUpdate, now).toMillis() / 3_600_000.0;
        long produced = (long) Math.floor(hourlyRate * elapsedHours);
        if (produced <= 0) {
            return;
        }
        double consumedHours = produced / hourlyRate;
        Instant newLastUpdate = lastUpdate.plusMillis((long) (consumedHours * 3_600_000));
        repository.applyProductionDelta(planetId, key, produced, lastUpdate, newLastUpdate);
    }

    /**
     * Debits all three ledgers or none of them. The upfront affordability check gives the
     * common case a clean "Not enough resources" message; the actual mutation still goes
     * through {@link #debitAtomically}, so a concurrent debit racing between the check and here
     * fails the same way rather than silently overdrawing the ledger.
     */
    @Transactional
    public void debit(Long planetId, ResourceCost cost) {
        PlanetResource metal = require(planetId, ResourceKey.METAL);
        PlanetResource crystal = require(planetId, ResourceKey.CRYSTAL);
        PlanetResource deuterium = require(planetId, ResourceKey.DEUTERIUM);

        if (!cost.isAffordable(metal.getAmount(), crystal.getAmount(), deuterium.getAmount())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources");
        }

        debitAtomically(planetId, ResourceKey.METAL, cost.metal());
        debitAtomically(planetId, ResourceKey.CRYSTAL, cost.crystal());
        debitAtomically(planetId, ResourceKey.DEUTERIUM, cost.deuterium());
    }

    @Transactional
    public void credit(Long planetId, ResourceCost amount) {
        creditAtomically(planetId, ResourceKey.METAL, amount.metal());
        creditAtomically(planetId, ResourceKey.CRYSTAL, amount.crystal());
        creditAtomically(planetId, ResourceKey.DEUTERIUM, amount.deuterium());
    }

    @Transactional
    public void debit(Long planetId, ResourceKey key, long amount) {
        if (amount <= 0) {
            return;
        }
        int updated = repository.debitIfSufficient(planetId, key, amount);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough " + key.getDisplayName());
        }
    }

    @Transactional
    public void credit(Long planetId, ResourceKey key, long amount) {
        creditAtomically(planetId, key, amount);
    }

    private void debitAtomically(Long planetId, ResourceKey key, long amount) {
        if (amount <= 0) {
            return;
        }
        int updated = repository.debitIfSufficient(planetId, key, amount);
        if (updated == 0) {
            // The upfront check passed, but a concurrent debit beat us to it between that check
            // and this statement - fail the same way an insufficient-funds check would, instead
            // of silently leaving the other two ledgers already-debited resources unspent.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources");
        }
    }

    private void creditAtomically(Long planetId, ResourceKey key, long amount) {
        if (amount <= 0) {
            return;
        }
        int updated = repository.creditAmount(planetId, key, amount);
        if (updated == 0) {
            throw new IllegalStateException("Planet %d has no %s ledger row".formatted(planetId, key));
        }
    }

    private PlanetResource require(Long planetId, ResourceKey key) {
        return repository.findByPlanetIdAndResourceKey(planetId, key)
                .orElseThrow(() -> new IllegalStateException("Planet %d has no %s ledger row".formatted(planetId, key)));
    }
}
