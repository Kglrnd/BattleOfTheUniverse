package de.kugi.dev.battleoftheuniverse.defense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TowerRepository extends JpaRepository<Tower, Long> {
    List<Tower> findByPlanetId(Long planetId);

    Optional<Tower> findByPlanetIdAndTowerKey(Long planetId, String towerKey);

    /** Total tower quantity per (owner, tower key) across every planet game-wide - used by the highscore. */
    @Query("""
            select p.ownerId as ownerId, t.towerKey as key, sum(t.quantity) as total
            from Tower t join Planet p on p.id = t.planetId
            group by p.ownerId, t.towerKey
            """)
    List<OwnerKeyTotal> sumQuantitiesByOwnerAndTowerKey();

    interface OwnerKeyTotal {
        Long getOwnerId();

        String getKey();

        long getTotal();
    }
}
