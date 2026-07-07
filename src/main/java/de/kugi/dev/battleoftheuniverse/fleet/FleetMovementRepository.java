package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FleetMovementRepository extends JpaRepository<FleetMovement, Long> {
    List<FleetMovement> findByOwnerId(Long ownerId);

    List<FleetMovement> findByArrivesAtBefore(Instant instant);
}
