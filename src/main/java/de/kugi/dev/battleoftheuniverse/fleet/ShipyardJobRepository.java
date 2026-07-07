package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShipyardJobRepository extends JpaRepository<ShipyardJob, Long> {
    Optional<ShipyardJob> findByPlanetId(Long planetId);

    List<ShipyardJob> findByEndsAtBefore(Instant instant);
}
