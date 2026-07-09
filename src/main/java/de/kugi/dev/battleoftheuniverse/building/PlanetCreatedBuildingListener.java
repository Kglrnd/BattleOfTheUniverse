package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.planet.PlanetCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanetCreatedBuildingListener {

    private final BuildingService buildingService;

    @ApplicationModuleListener
    void on(PlanetCreated event) {
        buildingService.initializeStarter(event.planetId());
    }
}
