package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.user.UserRegistered;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {

    private final PlanetService planetService;

    public UserRegistrationListener(PlanetService planetService) {
        this.planetService = planetService;
    }

    @ApplicationModuleListener
    void on(UserRegistered event) {
        planetService.createStarterPlanet(event.userId(), event.username());
    }
}
