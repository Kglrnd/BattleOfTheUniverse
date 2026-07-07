package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShipyardScheduler {

    private final FleetService fleetService;

    public ShipyardScheduler(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @Scheduled(fixedRate = 5000)
    public void completeDueBuilds() {
        fleetService.completeDueJobs();
    }
}
