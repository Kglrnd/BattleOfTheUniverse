package de.kugi.dev.battleoftheuniverse.building;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConstructionScheduler {

    private final BuildingService buildingService;

    public ConstructionScheduler(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @Scheduled(fixedRate = 5000)
    public void completeDueConstructions() {
        buildingService.completeDueJobs();
    }
}
