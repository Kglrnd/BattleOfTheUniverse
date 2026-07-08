package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long ORIGIN_ID = 10L;

    @Mock
    private ShipRepository shipRepository;
    @Mock
    private ShipyardJobRepository jobRepository;
    @Mock
    private FleetMovementRepository movementRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private PlanetService planetService;
    @Mock
    private ResearchService researchService;

    private FleetService service;

    private final Planet origin = new Planet("Origin", OWNER_ID, 1, 1, 1, PlanetClass.TEMPERATE);
    private final ShipDefinition colonyShip = new ShipDefinition(
            "colony_ship", "Colony Ship", "desc", 0, 100, 2500, 7500,
            new ResourceCost(10000, 20000, 10000), 3600);

    @BeforeEach
    void setUp() {
        service = new FleetService(shipRepository, jobRepository, movementRepository, catalogService,
                resourceService, planetService, researchService);
        origin.setId(ORIGIN_ID);
    }

    @Test
    void dispatchRejectsColonizeWithANonColonyShip() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, "cruiser", 1, FleetMissionType.COLONIZE, 1, 1, 2);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("colony ship");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchRejectsColonizeWhenTargetIsNotColonizable() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.isColonizable(1, 1, 2)).thenReturn(false);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, "colony_ship", 1, FleetMissionType.COLONIZE, 1, 1, 2);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("colonized");
    }

    @Test
    void dispatchAcceptsColonizeToAColonizableSlotAndConsumesTheShip() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.isColonizable(1, 1, 2)).thenReturn(true);
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "colony_ship"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "colony_ship", 1)));
        when(researchService.speedMultiplierFor(any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("colony_ship")).thenReturn(colonyShip);
        when(movementRepository.save(any(FleetMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, "colony_ship", 1, FleetMissionType.COLONIZE, 1, 1, 2);
        service.dispatch(OWNER_ID, request);

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getQuantity()).isZero();
    }

    @Test
    void dispatchRejectsStationWhenNoPlanetExistsAtTheTarget() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.empty());

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, "cruiser", 1, FleetMissionType.STATION, 1, 1, 2);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No planet");
    }

    @Test
    void dispatchRejectsStationAtAPlanetOwnedBySomeoneElse() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet foreignPlanet = new Planet("Not mine", OTHER_OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(foreignPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, "cruiser", 1, FleetMissionType.STATION, 1, 1, 2);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own planets");
    }

    @Test
    void completeDueMissionsFoundsAColonyForADueColonizeMovement() {
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, "colony_ship", 1, FleetMissionType.COLONIZE,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));

        service.completeDueMissions();

        verify(planetService).createColonyPlanetAt(eq(OWNER_ID), any(), eq(2), eq(5), eq(9));
        verify(movementRepository).delete(movement);
    }

    @Test
    void completeDueMissionsCreditsShipsToTheTargetPlanetForADueStationMovement() {
        Planet target = new Planet("Target", OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        target.setId(20L);
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, "cruiser", 3, FleetMissionType.STATION,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        when(planetService.findAtPosition(2, 5, 9)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(20L, "cruiser")).thenReturn(Optional.of(new Ship(20L, "cruiser", 4)));

        service.completeDueMissions();

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getQuantity()).isEqualTo(7);
        verify(movementRepository).delete(movement);
    }
}
