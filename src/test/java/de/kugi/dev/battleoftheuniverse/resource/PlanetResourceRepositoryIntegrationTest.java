package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the raw JPQL behind {@link PlanetResourceRepository}'s atomic debit/credit/production
 * queries against a real (H2) database - a Mockito-based {@code ResourceServiceTest} can't catch
 * a query syntax mistake or a wrong guard condition since the repository itself is mocked there.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanetResourceRepositoryIntegrationTest {

    private static final Long PLANET_ID = 42L;

    @Autowired
    private PlanetResourceRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void debitIfSufficientAppliesTheDecrementAndReturnsOneWhenAffordable() {
        repository.save(new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000));

        int updated = repository.debitIfSufficient(PLANET_ID, ResourceKey.METAL, 300);

        assertThat(updated).isEqualTo(1);
        assertThat(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL).orElseThrow().getAmount())
                .isEqualTo(700);
    }

    @Test
    void debitIfSufficientChangesNothingAndReturnsZeroWhenNotAffordable() {
        repository.save(new PlanetResource(PLANET_ID, ResourceKey.METAL, 100));

        int updated = repository.debitIfSufficient(PLANET_ID, ResourceKey.METAL, 300);

        assertThat(updated).isEqualTo(0);
        assertThat(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL).orElseThrow().getAmount())
                .isEqualTo(100);
    }

    @Test
    void creditAmountAppliesTheIncrement() {
        repository.save(new PlanetResource(PLANET_ID, ResourceKey.CRYSTAL, 500));

        int updated = repository.creditAmount(PLANET_ID, ResourceKey.CRYSTAL, 50);

        assertThat(updated).isEqualTo(1);
        assertThat(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.CRYSTAL).orElseThrow().getAmount())
                .isEqualTo(550);
    }

    @Test
    void applyProductionDeltaAppliesWhenTheLastUpdateGuardMatches() {
        PlanetResource resource = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        resource.setLastUpdate(Instant.now().minusSeconds(3600));
        repository.save(resource);
        // Force a genuine round-trip through the DB rather than serving the still-cached Java
        // object back from the persistence context: the column truncates Instant precision, so
        // the guard must be compared against what's actually stored - exactly what
        // ResourceService.applyOneProduction does in production (its own read is always the
        // first thing to touch that row in its transaction, so it never hits this cache).
        entityManager.flush();
        entityManager.clear();
        Instant storedLastUpdate = repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL).orElseThrow().getLastUpdate();
        Instant newLastUpdate = storedLastUpdate.plusSeconds(3600);

        int updated = repository.applyProductionDelta(PLANET_ID, ResourceKey.METAL, 30, storedLastUpdate, newLastUpdate);

        assertThat(updated).isEqualTo(1);
        PlanetResource reloaded = repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL).orElseThrow();
        assertThat(reloaded.getAmount()).isEqualTo(1030);
        assertThat(reloaded.getLastUpdate()).isEqualTo(newLastUpdate);
    }

    @Test
    void applyProductionDeltaIsANoOpWhenTheLastUpdateGuardIsStale() {
        // Simulates a concurrent tick having already advanced lastUpdate since this caller read it.
        Instant staleLastUpdate = Instant.now().minusSeconds(7200);
        Instant actualLastUpdate = Instant.now().minusSeconds(3600);
        PlanetResource resource = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        resource.setLastUpdate(actualLastUpdate);
        repository.save(resource);

        int updated = repository.applyProductionDelta(PLANET_ID, ResourceKey.METAL, 30, staleLastUpdate, Instant.now());

        assertThat(updated).isEqualTo(0);
        assertThat(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL).orElseThrow().getAmount())
                .isEqualTo(1000);
    }
}
