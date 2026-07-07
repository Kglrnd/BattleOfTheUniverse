package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.planet.PlanetCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PlanetCreatedBuildingListener {

    private final BuildingService buildingService;

    public PlanetCreatedBuildingListener(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @ApplicationModuleListener
    void on(PlanetCreated event) {
        buildingService.initializeStarter(event.planetId());
    }
}
