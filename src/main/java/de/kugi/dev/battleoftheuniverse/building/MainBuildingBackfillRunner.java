package de.kugi.dev.battleoftheuniverse.building;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One-time startup fixup for planets created before the main-building requirement system
 * existed: they already have starter resource buildings but no {@code main_building} row,
 * which would otherwise leave them unable to ever unlock the shipyard/research lab tier.
 * Idempotent, so it's safe to run on every startup.
 */
@Component
public class MainBuildingBackfillRunner implements ApplicationRunner {

    private final BuildingService buildingService;

    public MainBuildingBackfillRunner(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        buildingService.backfillMainBuilding();
    }
}
