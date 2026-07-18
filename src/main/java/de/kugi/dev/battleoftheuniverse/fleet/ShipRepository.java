package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {
    List<Ship> findByPlanetId(Long planetId);

    Optional<Ship> findByPlanetIdAndShipKey(Long planetId, String shipKey);

    /** Total ship quantity per (owner, ship key) across every planet game-wide - used by the highscore. */
    @Query("""
            select p.ownerId as ownerId, s.shipKey as key, sum(s.quantity) as total
            from Ship s join Planet p on p.id = s.planetId
            group by p.ownerId, s.shipKey
            """)
    List<OwnerKeyTotal> sumQuantitiesByOwnerAndShipKey();

    interface OwnerKeyTotal {
        Long getOwnerId();

        String getKey();

        long getTotal();
    }
}
