package de.kugi.dev.battleoftheuniverse.defense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TowerRepository extends JpaRepository<Tower, Long> {
    List<Tower> findByPlanetId(Long planetId);

    Optional<Tower> findByPlanetIdAndTowerKey(Long planetId, String towerKey);
}
