package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.planet.PlanetCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanetCreatedResourceListener {

    private final ResourceService resourceService;

    @ApplicationModuleListener
    void on(PlanetCreated event) {
        resourceService.initializeStarter(event.planetId());
    }
}
