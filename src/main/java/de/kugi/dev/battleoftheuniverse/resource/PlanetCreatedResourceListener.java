package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.planet.PlanetCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PlanetCreatedResourceListener {

    private final ResourceService resourceService;

    public PlanetCreatedResourceListener(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @ApplicationModuleListener
    void on(PlanetCreated event) {
        resourceService.initializeStarter(event.planetId());
    }
}
