package de.kugi.dev.battleoftheuniverse.fleet;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FleetMissionScheduler {

    private final FleetService fleetService;

    @Scheduled(fixedRate = 5000)
    public void completeDueMissions() {
        fleetService.completeDueMissions();
    }
}
