package de.kugi.dev.battleoftheuniverse.fleet;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FleetMissionScheduler {

    private final FleetService fleetService;

    public FleetMissionScheduler(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @Scheduled(fixedRate = 5000)
    public void completeDueMissions() {
        fleetService.completeDueMissions();
    }
}
