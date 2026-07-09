package de.kugi.dev.battleoftheuniverse.fleet;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipyardScheduler {

    private final FleetService fleetService;

    @Scheduled(fixedRate = 5000)
    public void completeDueBuilds() {
        fleetService.completeDueJobs();
    }
}
