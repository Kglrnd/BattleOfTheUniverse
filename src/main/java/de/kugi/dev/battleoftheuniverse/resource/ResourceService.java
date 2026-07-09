package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

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

    /**
     * Credits whatever whole units of production have accrued since the resource's
     * {@code lastUpdate}, then advances {@code lastUpdate} only by the time that was
     * actually "spent" producing those whole units — the fractional remainder carries
     * over to the next call instead of being lost, regardless of how often this runs.
     */
    @Transactional
    public void applyProduction(Long planetId, ResourceKey key, double hourlyRate) {
        if (hourlyRate <= 0) {
            return;
        }
        PlanetResource resource = require(planetId, key);
        Instant now = Instant.now();
        double elapsedHours = java.time.Duration.between(resource.getLastUpdate(), now).toMillis() / 3_600_000.0;
        long produced = (long) Math.floor(hourlyRate * elapsedHours);
        if (produced <= 0) {
            return;
        }
        double consumedHours = produced / hourlyRate;
        resource.setAmount(resource.getAmount() + produced);
        resource.setLastUpdate(resource.getLastUpdate().plusMillis((long) (consumedHours * 3_600_000)));
        repository.save(resource);
    }

    @Transactional
    public void debit(Long planetId, ResourceCost cost) {
        PlanetResource metal = require(planetId, ResourceKey.METAL);
        PlanetResource crystal = require(planetId, ResourceKey.CRYSTAL);
        PlanetResource deuterium = require(planetId, ResourceKey.DEUTERIUM);

        if (!cost.isAffordable(metal.getAmount(), crystal.getAmount(), deuterium.getAmount())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough resources");
        }

        metal.setAmount(metal.getAmount() - cost.metal());
        crystal.setAmount(crystal.getAmount() - cost.crystal());
        deuterium.setAmount(deuterium.getAmount() - cost.deuterium());
        repository.save(metal);
        repository.save(crystal);
        repository.save(deuterium);
    }

    private PlanetResource require(Long planetId, ResourceKey key) {
        return repository.findByPlanetIdAndResourceKey(planetId, key)
                .orElseThrow(() -> new IllegalStateException("Planet %d has no %s ledger row".formatted(planetId, key)));
    }
}
