package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlanetResourceRepository extends JpaRepository<PlanetResource, Long> {
    List<PlanetResource> findByPlanetId(Long planetId);

    Optional<PlanetResource> findByPlanetIdAndResourceKey(Long planetId, ResourceKey resourceKey);

    /**
     * Atomically decrements the ledger row only if it can afford {@code amount} - the
     * {@code amount >= :amount} guard is checked and applied in the same statement, so two
     * concurrent debits can't both read a sufficient balance and then both subtract, driving the
     * amount negative (or double-spending the same resources). Returns 0 (nothing updated) if the
     * row doesn't exist or can no longer afford it - the caller decides how to report that.
     */
    @Modifying(clearAutomatically = true)
    @Query("update PlanetResource r set r.amount = r.amount - :amount " +
            "where r.planetId = :planetId and r.resourceKey = :key and r.amount >= :amount")
    int debitIfSufficient(@Param("planetId") Long planetId, @Param("key") ResourceKey key, @Param("amount") long amount);

    /** Atomically increments the ledger row - safe to run concurrently with {@link #debitIfSufficient} or another credit. */
    @Modifying(clearAutomatically = true)
    @Query("update PlanetResource r set r.amount = r.amount + :amount where r.planetId = :planetId and r.resourceKey = :key")
    int creditAmount(@Param("planetId") Long planetId, @Param("key") ResourceKey key, @Param("amount") long amount);

    /**
     * Atomically applies a production tick: adds {@code produced} to the amount (so it composes
     * correctly with a concurrent {@link #debitIfSufficient}/{@link #creditAmount} instead of
     * clobbering it the way a full read-modify-write {@code save()} would) and advances
     * {@code lastUpdate}, guarded by the {@code lastUpdate} value the caller's elapsed-time
     * calculation was based on.
     */
    @Modifying(clearAutomatically = true)
    @Query("update PlanetResource r set r.amount = r.amount + :produced, r.lastUpdate = :newLastUpdate " +
            "where r.planetId = :planetId and r.resourceKey = :key and r.lastUpdate = :expectedLastUpdate")
    int applyProductionDelta(@Param("planetId") Long planetId, @Param("key") ResourceKey key,
                              @Param("produced") long produced, @Param("expectedLastUpdate") Instant expectedLastUpdate,
                              @Param("newLastUpdate") Instant newLastUpdate);
}
