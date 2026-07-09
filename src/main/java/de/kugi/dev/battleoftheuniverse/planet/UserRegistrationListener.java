package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.user.UserRegistered;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final PlanetService planetService;

    @ApplicationModuleListener
    void on(UserRegistered event) {
        planetService.createStarterPlanet(event.userId(), event.username());
    }
}
