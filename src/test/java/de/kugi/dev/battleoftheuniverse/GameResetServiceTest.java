package de.kugi.dev.battleoftheuniverse;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import de.kugi.dev.battleoftheuniverse.message.MessageService;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResetServiceTest {

    @Mock
    private PlanetService planetService;
    @Mock
    private BuildingService buildingService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private ResearchService researchService;
    @Mock
    private FleetService fleetService;
    @Mock
    private MessageService messageService;

    private GameResetService service;

    @BeforeEach
    void setUp() {
        service = new GameResetService(planetService, buildingService, resourceService, researchService, fleetService, messageService);
    }

    @Test
    void resetGameWipesEveryModuleThenReseedsEachSurvivingHomeworld() {
        Planet homeworldOne = new Planet("Alice's Homeworld", 1L, 1, 1, 1, PlanetClass.TEMPERATE);
        homeworldOne.setId(10L);
        Planet homeworldTwo = new Planet("Bob's Homeworld", 2L, 2, 2, 2, PlanetClass.TEMPERATE);
        homeworldTwo.setId(20L);
        when(planetService.resetAllToHomeworldsOnly()).thenReturn(List.of(homeworldOne, homeworldTwo));

        service.resetGame();

        verify(messageService).wipeAll();
        verify(fleetService).wipeAll();
        verify(researchService).wipeAll();
        verify(buildingService).wipeAll();
        verify(resourceService).wipeAll();
        verify(planetService).resetAllToHomeworldsOnly();

        verify(buildingService).initializeStarter(10L);
        verify(resourceService).initializeStarter(10L);
        verify(buildingService).initializeStarter(20L);
        verify(resourceService).initializeStarter(20L);

        // Every module's data must be gone before the homeworld reset runs, and the
        // homeworld reset must happen before its starter state is reseeded - otherwise a
        // stale row (e.g. a leftover resource ledger) could survive the wipe.
        InOrder order = inOrder(buildingService, resourceService, planetService);
        order.verify(buildingService).wipeAll();
        order.verify(resourceService).wipeAll();
        order.verify(planetService).resetAllToHomeworldsOnly();
        order.verify(buildingService).initializeStarter(10L);
        order.verify(resourceService).initializeStarter(10L);
    }
}
