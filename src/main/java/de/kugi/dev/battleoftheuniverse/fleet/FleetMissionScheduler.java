package de.kugi.dev.battleoftheuniverse.fleet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FleetMissionScheduler {

    private final FleetService fleetService;

    /**
     * Each due movement is completed in its own transaction (see
     * {@link FleetService#completeOneMovement}), and a failure is caught and recovered here
     * rather than propagating - so one bad arrival can't roll back and permanently re-block
     * every other due movement in the sweep.
     */
    @Scheduled(fixedRate = 5000)
    public void completeDueMissions() {
        for (Long movementId : fleetService.dueMovementIds()) {
            try {
                fleetService.completeOneMovement(movementId);
            } catch (Exception e) {
                log.error("Failed to complete fleet movement {}, returning it home instead", movementId, e);
                fleetService.recoverFailedMovement(movementId);
            }
        }
    }
}
