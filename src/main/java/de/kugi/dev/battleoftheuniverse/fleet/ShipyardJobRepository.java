package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ShipyardJobRepository extends JpaRepository<ShipyardJob, Long> {
    /** The planet's whole build queue in execution order - at most 1 row unless the level-6+ pipeline is in use. */
    List<ShipyardJob> findByPlanetIdOrderByEndsAtAsc(Long planetId);

    List<ShipyardJob> findByEndsAtBefore(Instant instant);
}
