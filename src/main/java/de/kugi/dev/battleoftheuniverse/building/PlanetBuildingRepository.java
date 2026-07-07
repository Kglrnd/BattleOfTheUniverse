package de.kugi.dev.battleoftheuniverse.building;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanetBuildingRepository extends JpaRepository<PlanetBuilding, Long> {
    List<PlanetBuilding> findByPlanetId(Long planetId);

    Optional<PlanetBuilding> findByPlanetIdAndBuildingKey(Long planetId, String buildingKey);

    List<PlanetBuilding> findByLevelGreaterThan(int level);
}
