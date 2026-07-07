package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {
    List<Ship> findByPlanetId(Long planetId);

    Optional<Ship> findByPlanetIdAndShipKey(Long planetId, String shipKey);
}
