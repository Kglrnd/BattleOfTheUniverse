package de.kugi.dev.battleoftheuniverse.building;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanetBuildingRepository extends JpaRepository<PlanetBuilding, Long> {
    List<PlanetBuilding> findByPlanetId(Long planetId);

    Optional<PlanetBuilding> findByPlanetIdAndBuildingKey(Long planetId, String buildingKey);

    List<PlanetBuilding> findByLevelGreaterThan(int level);

    /** Total building level per (owner, building key) across every planet game-wide - used by the highscore. */
    @Query("""
            select p.ownerId as ownerId, pb.buildingKey as key, sum(pb.level) as total
            from PlanetBuilding pb join Planet p on p.id = pb.planetId
            group by p.ownerId, pb.buildingKey
            """)
    List<OwnerKeyTotal> sumLevelsByOwnerAndBuildingKey();

    interface OwnerKeyTotal {
        Long getOwnerId();

        String getKey();

        long getTotal();
    }
}
