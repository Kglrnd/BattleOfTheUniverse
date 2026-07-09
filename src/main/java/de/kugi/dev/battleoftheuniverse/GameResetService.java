package de.kugi.dev.battleoftheuniverse;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import de.kugi.dev.battleoftheuniverse.message.MessageService;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-triggered full game reset: wipes buildings/resources/research/fleet/defenses/messages
 * game-wide, deletes every colony, redistributes homeworlds to new random positions, and
 * reseeds each surviving homeworld's starter state. Lives in the base package (not inside
 * any of the {@code @ApplicationModule} packages) for the same reason as {@link DevWorldSeeder}
 * — it wires across module boundaries that individual modules aren't allowed to cross
 * themselves. Unlike {@link DevWorldSeeder}, this is a real (non-dev-only) admin feature.
 */
@Service
@RequiredArgsConstructor
public class GameResetService {

    private final PlanetService planetService;
    private final BuildingService buildingService;
    private final ResourceService resourceService;
    private final ResearchService researchService;
    private final FleetService fleetService;
    private final MessageService messageService;
    private final DefenseService defenseService;

    @Transactional
    public void resetGame() {
        messageService.wipeAll();
        fleetService.wipeAll();
        defenseService.wipeAll();
        researchService.wipeAll();
        buildingService.wipeAll();
        resourceService.wipeAll();

        for (Planet homeworld : planetService.resetAllToHomeworldsOnly()) {
            buildingService.initializeStarter(homeworld.getId());
            resourceService.initializeStarter(homeworld.getId());
        }
    }
}
