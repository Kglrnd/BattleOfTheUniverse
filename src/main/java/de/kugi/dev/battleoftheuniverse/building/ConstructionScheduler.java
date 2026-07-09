package de.kugi.dev.battleoftheuniverse.building;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstructionScheduler {

    private final BuildingService buildingService;

    @Scheduled(fixedRate = 5000)
    public void completeDueConstructions() {
        buildingService.completeDueJobs();
    }
}
