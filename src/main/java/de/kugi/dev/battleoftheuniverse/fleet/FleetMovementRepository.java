package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FleetMovementRepository extends JpaRepository<FleetMovement, Long> {
    List<FleetMovement> findByOwnerId(Long ownerId);

    List<FleetMovement> findByArrivesAtBefore(Instant instant);

    /**
     * Every in-flight movement (sent by anyone) currently headed at one of this owner's own
     * planets - filtered in the DB instead of loading every movement in the game and matching
     * coordinates in memory.
     */
    @Query("""
            select m from FleetMovement m
            where exists (
                select 1 from Planet p
                where p.ownerId = :ownerId
                  and p.galaxy = m.targetGalaxy
                  and p.system = m.targetSystem
                  and p.position = m.targetPosition
            )
            """)
    List<FleetMovement> findIncomingForOwner(@Param("ownerId") Long ownerId);
}
