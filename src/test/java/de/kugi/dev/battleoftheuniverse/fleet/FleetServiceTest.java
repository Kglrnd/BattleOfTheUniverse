package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionsRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementMapperImpl;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ResourceQuantity;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.research.dto.DriveOption;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceMapperImpl;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long ORIGIN_ID = 10L;
    private static final String DRIVE_KEY = "chemical_drive";

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
    @Mock
    private BuildingService buildingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher events;

    private FleetService service;

    private final Planet origin = new Planet("Origin", OWNER_ID, 1, 1, 1, PlanetClass.TEMPERATE);
    private final ShipDefinition colonyShip = new ShipDefinition(
            "colony_ship", "Colony Ship", "desc", 0, 100, 2500, 7500, 6,
            new ResourceCost(10000, 20000, 10000), 3600, 0, List.of());
    private final ShipDefinition cruiser = new ShipDefinition(
            "cruiser", "Cruiser", "desc", 400, 200, 15000, 50, 4,
            new ResourceCost(20000, 7000, 2000), 1800, 29, List.of());
    private final ShipDefinition lightFighter = new ShipDefinition(
            "light_fighter", "Light Fighter", "desc", 50, 10, 12500, 50, 1,
            new ResourceCost(3000, 1000, 0), 60, 4, List.of());

    @BeforeEach
    void setUp() {
        service = new FleetService(shipRepository, jobRepository, movementRepository, catalogService,
                resourceService, planetService, researchService, buildingService, userRepository, events, new FleetMovementMapperImpl(),
                new ResourceMapperImpl());
        origin.setId(ORIGIN_ID);
    }

    @Test
    void dispatchRejectsColonizeWithoutAColonyShip() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.COLONIZE, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("colony ship");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchRejectsColonizeWhenTargetIsNotColonizable() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.isColonizable(1, 1, 2)).thenReturn(false);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("colony_ship", 1)),
                FleetMissionType.COLONIZE, 1, 1, 2, DRIVE_KEY);

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
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("colony_ship")).thenReturn(colonyShip);
        when(movementRepository.save(any(FleetMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("colony_ship", 1)),
                FleetMissionType.COLONIZE, 1, 1, 2, DRIVE_KEY);
        service.dispatch(OWNER_ID, request);

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getQuantity()).isZero();
    }

    @Test
    void dispatchAcceptsAMixedFleetAndDebitsEveryShipType() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        target.setId(20L);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "cruiser"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "cruiser", 5)));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "light_fighter"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "light_fighter", 10)));
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        when(catalogService.ship("light_fighter")).thenReturn(lightFighter);
        when(movementRepository.save(any(FleetMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("cruiser", 3), new ShipQuantity("light_fighter", 4)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);
        var view = service.dispatch(OWNER_ID, request);

        assertThat(view.ships()).containsExactlyInAnyOrder(
                new ShipQuantity("cruiser", 3), new ShipQuantity("light_fighter", 4));
        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository, times(2)).save(shipCaptor.capture());
        assertThat(shipCaptor.getAllValues())
                .extracting(Ship::getShipKey, Ship::getQuantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("cruiser", 2),
                        org.assertj.core.groups.Tuple.tuple("light_fighter", 6));
    }

    @Test
    void dispatchRejectsDuplicateShipKeysInTheManifest() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        // Manifest-only checks run before target resolution, so no findAtPosition stub is needed.

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("cruiser", 3), new ShipQuantity("cruiser", 2)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Duplicate ship type");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchFailsCleanlyWhenOneShipTypeIsUnderStocked() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "cruiser"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "cruiser", 5)));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "light_fighter"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "light_fighter", 1)));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("cruiser", 3), new ShipQuantity("light_fighter", 4)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("light_fighter");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchRejectsAnUnresearchedOrOutOfScopeDrive() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.isColonizable(1, 1, 2)).thenReturn(true);
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "colony_ship"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "colony_ship", 1)));
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.empty());
        when(catalogService.ship("colony_ship")).thenReturn(colonyShip);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("colony_ship", 1)),
                FleetMissionType.COLONIZE, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Chosen drive");
    }

    @Test
    void dispatchRejectsStationWhenNoPlanetExistsAtTheTarget() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.empty());

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No planet");
    }

    @Test
    void dispatchRejectsStationAtAPlanetOwnedBySomeoneElse() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet foreignPlanet = new Planet("Not mine", OTHER_OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(foreignPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own planets");
    }

    @Test
    void dispatchRejectsAttackingYourOwnPlanet() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet ownPlanet = new Planet("Also mine", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(ownPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.ATTACK, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own planet");
    }

    @Test
    void dispatchRejectsAttackWhenNoPlanetExistsAtTheTarget() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.empty());

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.ATTACK, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No planet");
    }

    @Test
    void dispatchRejectsEspionageWithoutAProbe() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.ESPIONAGE, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("espionage probe");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchRejectsSpyingOnYourOwnPlanet() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet ownPlanet = new Planet("Also mine", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(ownPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("espionage_probe", 1)),
                FleetMissionType.ESPIONAGE, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own planet");
    }

    @Test
    void completeDueMissionsPublishesEspionageResolvedAndReturnsTheProbeToOrigin() {
        Planet target = new Planet("Target", OTHER_OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        target.setId(20L);
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, Map.of("espionage_probe", 1), FleetMissionType.ESPIONAGE,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        when(planetService.findAtPosition(2, 5, 9)).thenReturn(Optional.of(target));
        when(researchService.levelOf(OWNER_ID, "espionage_technology")).thenReturn(0);
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "espionage_probe")).thenReturn(Optional.of(new Ship(ORIGIN_ID, "espionage_probe", 4)));
        // Only consulted on a successful roll - stubbed leniently since the outcome is random.
        lenient().when(shipRepository.findByPlanetId(20L)).thenReturn(List.of());
        lenient().when(resourceService.raw(20L)).thenReturn(List.of());

        service.completeDueMissions();

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(EspionageResolved.class);
        verify(events).publishEvent(eventCaptor.capture());
        EspionageResolved published = eventCaptor.getValue();
        assertThat(published.attackerId()).isEqualTo(OWNER_ID);
        assertThat(published.defenderId()).isEqualTo(OTHER_OWNER_ID);
        assertThat(published.targetPlanetId()).isEqualTo(20L);
        assertThat(published.targetPlanetName()).isEqualTo("Target");

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getPlanetId()).isEqualTo(ORIGIN_ID);
        assertThat(shipCaptor.getValue().getShipKey()).isEqualTo("espionage_probe");
        assertThat(shipCaptor.getValue().getQuantity()).isEqualTo(5);
        verify(movementRepository).delete(movement);
    }

    @Test
    void completeDueMissionsFoundsAColonyForADueColonizeMovement() {
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, Map.of("colony_ship", 1), FleetMissionType.COLONIZE,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        Planet colony = new Planet("Colony", OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        colony.setId(30L);
        when(planetService.createColonyPlanetAt(eq(OWNER_ID), any(), eq(2), eq(5), eq(9))).thenReturn(colony);

        service.completeDueMissions();

        verify(planetService).createColonyPlanetAt(eq(OWNER_ID), any(), eq(2), eq(5), eq(9));
        verify(movementRepository).delete(movement);
    }

    @Test
    void completeDueMissionsStationsEscortShipsAtTheNewColonyAndConsumesOnlyTheColonyShip() {
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID,
                Map.of("colony_ship", 1, "cruiser", 4), FleetMissionType.COLONIZE,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        Planet colony = new Planet("Colony", OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        colony.setId(30L);
        when(planetService.createColonyPlanetAt(eq(OWNER_ID), any(), eq(2), eq(5), eq(9))).thenReturn(colony);
        when(shipRepository.findByPlanetIdAndShipKey(30L, "cruiser")).thenReturn(Optional.empty());

        service.completeDueMissions();

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getPlanetId()).isEqualTo(30L);
        assertThat(shipCaptor.getValue().getShipKey()).isEqualTo("cruiser");
        assertThat(shipCaptor.getValue().getQuantity()).isEqualTo(4);
        verify(movementRepository).delete(movement);
    }

    @Test
    void completeDueMissionsCreditsShipsToTheTargetPlanetForADueStationMovement() {
        Planet target = new Planet("Target", OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        target.setId(20L);
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, Map.of("cruiser", 3), FleetMissionType.STATION,
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

    @Test
    void completeDueMissionsPublishesAttackArrivedForADueAttackMovementWithoutCreditingShipsBack() {
        Planet target = new Planet("Target", OTHER_OWNER_ID, 9, 9, 9, PlanetClass.TEMPERATE);
        target.setId(20L);
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, Map.of("cruiser", 5), FleetMissionType.ATTACK,
                9, 9, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        when(planetService.findAtPosition(9, 9, 9)).thenReturn(Optional.of(target));

        service.completeDueMissions();

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(AttackArrived.class);
        verify(events).publishEvent(eventCaptor.capture());
        AttackArrived published = eventCaptor.getValue();
        assertThat(published.attackerId()).isEqualTo(OWNER_ID);
        assertThat(published.attackerOriginPlanetId()).isEqualTo(ORIGIN_ID);
        assertThat(published.defenderId()).isEqualTo(OTHER_OWNER_ID);
        assertThat(published.defenderPlanetId()).isEqualTo(20L);
        assertThat(published.defenderPlanetName()).isEqualTo("Target");
        assertThat(published.attackingShips()).containsExactlyEntriesOf(Map.of("cruiser", 5));

        // Resolving losses/survivors is combat's job now, not fleet's - no ship credited here.
        verify(shipRepository, never()).save(any());
        verify(movementRepository).delete(movement);
    }

    @Test
    void listIncomingReturnsOnlyMovementsTargetingMyPlanetsWithResolvedNames() {
        Planet myPlanet = new Planet("My Homeworld", OWNER_ID, 3, 7, 4, PlanetClass.TEMPERATE);
        myPlanet.setId(50L);
        when(planetService.listMine(OWNER_ID)).thenReturn(List.of(myPlanet));

        FleetMovement attackOnMe = new FleetMovement(99L, OTHER_OWNER_ID, Map.of("cruiser", 5), FleetMissionType.ATTACK,
                3, 7, 4, Instant.now(), Instant.now().plusSeconds(60));
        FleetMovement unrelatedMovement = new FleetMovement(77L, OTHER_OWNER_ID, Map.of("cruiser", 2), FleetMissionType.STATION,
                1, 1, 1, Instant.now(), Instant.now().plusSeconds(60));
        when(movementRepository.findAll()).thenReturn(List.of(attackOnMe, unrelatedMovement));

        User attacker = new User("raider", "raider@example.com", "hash");
        attacker.setId(OTHER_OWNER_ID);
        when(userRepository.findAllById(any())).thenReturn(List.of(attacker));

        List<de.kugi.dev.battleoftheuniverse.fleet.dto.IncomingMovementView> incoming = service.listIncoming(OWNER_ID);

        assertThat(incoming).hasSize(1);
        assertThat(incoming.getFirst().missionType()).isEqualTo(FleetMissionType.ATTACK);
        assertThat(incoming.getFirst().ships()).containsExactly(new ShipQuantity("cruiser", 5));
        assertThat(incoming.getFirst().targetPlanetId()).isEqualTo(50L);
        assertThat(incoming.getFirst().targetPlanetName()).isEqualTo("My Homeworld");
        assertThat(incoming.getFirst().originOwnerUsername()).isEqualTo("raider");
    }

    @Test
    void listDriveOptionsReturnsEveryEligibleDriveWithItsOwnEta() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        when(researchService.listAvailableDrives(OWNER_ID, DriveScope.SYSTEM)).thenReturn(List.of(
                new DriveOption("chemical_drive", "Chemical Drive", DriveScope.SYSTEM, 20, 2.0),
                new DriveOption("ion_drive", "Ion Drive", DriveScope.SYSTEM, 20, 1.9)
        ));

        DriveOptionsRequest request = new DriveOptionsRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 5)), 1, 1, 3001);
        List<DriveOptionView> options = service.listDriveOptions(OWNER_ID, request);

        assertThat(options).hasSize(2);
        assertThat(options).extracting(DriveOptionView::key).containsExactly("chemical_drive", "ion_drive");
        DriveOptionView chemical = options.getFirst();
        assertThat(chemical.speedMultiplier()).isEqualTo(2.0);
        assertThat(chemical.etaSeconds()).isLessThan(options.get(1).etaSeconds());
    }

    @Test
    void listDriveOptionsUsesTheSlowestShipInAMixedFleet() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        when(catalogService.ship("colony_ship")).thenReturn(colonyShip);
        when(researchService.listAvailableDrives(OWNER_ID, DriveScope.SYSTEM)).thenReturn(List.of(
                new DriveOption("chemical_drive", "Chemical Drive", DriveScope.SYSTEM, 20, 2.0)
        ));

        // cruiser is much faster (15000) than colony_ship (2500) - the fleet must travel at
        // the colony ship's pace.
        DriveOptionsRequest mixedRequest = new DriveOptionsRequest(ORIGIN_ID,
                List.of(new ShipQuantity("cruiser", 5), new ShipQuantity("colony_ship", 1)), 1, 1, 3001);
        DriveOptionsRequest cruiserOnlyRequest = new DriveOptionsRequest(ORIGIN_ID,
                List.of(new ShipQuantity("cruiser", 5)), 1, 1, 3001);

        long mixedEta = service.listDriveOptions(OWNER_ID, mixedRequest).getFirst().etaSeconds();
        long cruiserOnlyEta = service.listDriveOptions(OWNER_ID, cruiserOnlyRequest).getFirst().etaSeconds();

        assertThat(mixedEta).isGreaterThan(cruiserOnlyEta);
    }

    @Test
    void dispatchDebitsHydrogenFuelBasedOnDistanceAndDrive() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "cruiser"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "cruiser", 5)));
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        when(movementRepository.save(any(FleetMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 3)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);
        service.dispatch(OWNER_ID, request);

        // distance = POSITION_STEP (100) for a same-system 1-position hop; hops = 100/1000 = 0.1;
        // consumptionRate = cruiser.hydrogenConsumption(4) * 3 = 12; fuel = ceil(12 * 0.1 / 1.0) = 2.
        verify(resourceService).debit(ORIGIN_ID, ResourceKey.HYDROGEN, 2L);
    }

    @Test
    void dispatchFailsCleanlyWhenHydrogenIsInsufficient() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "cruiser"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "cruiser", 5)));
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Not enough Hydrogen"))
                .when(resourceService).debit(eq(ORIGIN_ID), eq(ResourceKey.HYDROGEN), anyLong());

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 3)),
                FleetMissionType.STATION, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Hydrogen");
        verify(shipRepository, never()).save(any());
    }

    @Test
    void dispatchRejectsTransportToAPlanetOwnedBySomeoneElse() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet foreignPlanet = new Planet("Not mine", OTHER_OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(foreignPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.TRANSPORT, 1, 1, 2, DRIVE_KEY, List.of());

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own planets");
    }

    @Test
    void dispatchRejectsTransportingEnergy() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.TRANSPORT, 1, 1, 2, DRIVE_KEY,
                List.of(new ResourceQuantity(ResourceKey.ENERGY, 10)));

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Energy");
    }

    @Test
    void dispatchRejectsTransportCargoExceedingFleetCapacity() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(catalogService.ship("cruiser")).thenReturn(cruiser);

        // cruiser cargoCapacity is 50; requesting 100 metal exceeds a single cruiser's capacity.
        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.TRANSPORT, 1, 1, 2, DRIVE_KEY,
                List.of(new ResourceQuantity(ResourceKey.METAL, 100)));

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void dispatchDebitsTransportCargoAtTheOrigin() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet target = new Planet("Target", OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(ORIGIN_ID, "cruiser"))
                .thenReturn(Optional.of(new Ship(ORIGIN_ID, "cruiser", 5)));
        when(researchService.speedMultiplierForDrive(any(), any(), any())).thenReturn(Optional.of(1.0));
        when(catalogService.ship("cruiser")).thenReturn(cruiser);
        when(movementRepository.save(any(FleetMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("cruiser", 1)),
                FleetMissionType.TRANSPORT, 1, 1, 2, DRIVE_KEY,
                List.of(new ResourceQuantity(ResourceKey.METAL, 30), new ResourceQuantity(ResourceKey.CRYSTAL, 10)));
        service.dispatch(OWNER_ID, request);

        verify(resourceService).debit(ORIGIN_ID, ResourceKey.METAL, 30L);
        verify(resourceService).debit(ORIGIN_ID, ResourceKey.CRYSTAL, 10L);
    }

    @Test
    void completeDueMissionsDeliversCargoAndStationsShipsForADueTransportMovement() {
        Planet target = new Planet("Target", OWNER_ID, 2, 5, 9, PlanetClass.TEMPERATE);
        target.setId(20L);
        Map<ResourceKey, Long> cargo = Map.of(ResourceKey.METAL, 30L, ResourceKey.CRYSTAL, 10L);
        FleetMovement movement = new FleetMovement(ORIGIN_ID, OWNER_ID, Map.of("cruiser", 1), cargo, FleetMissionType.TRANSPORT,
                2, 5, 9, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(movementRepository.findByArrivesAtBefore(any())).thenReturn(List.of(movement));
        when(planetService.findAtPosition(2, 5, 9)).thenReturn(Optional.of(target));
        when(shipRepository.findByPlanetIdAndShipKey(20L, "cruiser")).thenReturn(Optional.empty());

        service.completeDueMissions();

        var shipCaptor = org.mockito.ArgumentCaptor.forClass(Ship.class);
        verify(shipRepository).save(shipCaptor.capture());
        assertThat(shipCaptor.getValue().getPlanetId()).isEqualTo(20L);
        assertThat(shipCaptor.getValue().getShipKey()).isEqualTo("cruiser");
        assertThat(shipCaptor.getValue().getQuantity()).isEqualTo(1);

        verify(resourceService).credit(20L, ResourceKey.METAL, 30L);
        verify(resourceService).credit(20L, ResourceKey.CRYSTAL, 10L);
        verify(movementRepository).delete(movement);
    }

    @Test
    void dispatchRejectsBombsAndInvasionUnitsSentTogether() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(
                new ShipQuantity("orbital_bomb", 1), new ShipQuantity("invasion_unit", 1), new ShipQuantity("galaxy_class", 1)),
                FleetMissionType.BOMBARD, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be sent together");
    }

    @Test
    void dispatchRejectsABombFlyingWithoutAnyEscort() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID, List.of(new ShipQuantity("orbital_bomb", 1)),
                FleetMissionType.BOMBARD, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("without an escort");
    }

    @Test
    void dispatchRejectsABombWithoutAGalaxyClassEscort() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("orbital_bomb", 1), new ShipQuantity("cruiser", 5)),
                FleetMissionType.BOMBARD, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Galaxy Class escort");
    }

    @Test
    void dispatchRejectsBombardingAHomeworld() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet homeworld = new Planet("Enemy Homeworld", OTHER_OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        homeworld.setHomeworld(true);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(homeworld));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("orbital_bomb", 1), new ShipQuantity("galaxy_class", 1)),
                FleetMissionType.BOMBARD, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Homeworlds");
    }

    @Test
    void dispatchRejectsInvadingAPlanetAlreadyDestroyed() {
        when(planetService.getOwned(ORIGIN_ID, OWNER_ID)).thenReturn(origin);
        Planet destroyedPlanet = new Planet("Ruins", OTHER_OWNER_ID, 1, 1, 2, PlanetClass.TEMPERATE);
        destroyedPlanet.setDestroyed(true);
        when(planetService.findAtPosition(1, 1, 2)).thenReturn(Optional.of(destroyedPlanet));

        DispatchRequest request = new DispatchRequest(ORIGIN_ID,
                List.of(new ShipQuantity("invasion_unit", 1), new ShipQuantity("galaxy_class", 1)),
                FleetMissionType.INVADE, 1, 1, 2, DRIVE_KEY);

        assertThatThrownBy(() -> service.dispatch(OWNER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("destroyed");
    }

    @Test
    void listForPlanetReportsMissingRequirementsForAGatedShip() {
        ShipDefinition galaxyClass = new ShipDefinition("galaxy_class", "Galaxy Class", "desc", 1800, 1800, 8000, 300, 12,
                new ResourceCost(150000, 60000, 20000), 10800, 220,
                List.of(new de.kugi.dev.battleoftheuniverse.catalog.Requirement(
                        de.kugi.dev.battleoftheuniverse.catalog.RequirementType.BUILDING, "shipyard", 10)));
        when(catalogService.ships()).thenReturn(List.of(galaxyClass));
        when(buildingService.levelOf(ORIGIN_ID, "shipyard")).thenReturn(3);
        de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition shipyardDefinition =
                new de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition("shipyard", "Shipyard", "desc",
                        new ResourceCost(200, 100, 0), 1.15, 60, de.kugi.dev.battleoftheuniverse.catalog.ResourceKey.NONE, 0, 5, List.of());
        when(catalogService.building("shipyard")).thenReturn(shipyardDefinition);

        var views = service.listForPlanet(ORIGIN_ID, OWNER_ID);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().unlocked()).isFalse();
        assertThat(views.getFirst().missingRequirements()).hasSize(1);
        assertThat(views.getFirst().missingRequirements().getFirst().requiredLevel()).isEqualTo(10);
        assertThat(views.getFirst().missingRequirements().getFirst().currentLevel()).isEqualTo(3);
    }

    @Test
    void queueShipRejectsBuildingAGatedShipBelowItsRequirement() {
        ShipDefinition galaxyClass = new ShipDefinition("galaxy_class", "Galaxy Class", "desc", 1800, 1800, 8000, 300, 12,
                new ResourceCost(150000, 60000, 20000), 10800, 220,
                List.of(new de.kugi.dev.battleoftheuniverse.catalog.Requirement(
                        de.kugi.dev.battleoftheuniverse.catalog.RequirementType.BUILDING, "shipyard", 10)));
        when(catalogService.ship("galaxy_class")).thenReturn(galaxyClass);
        when(buildingService.levelOf(ORIGIN_ID, "shipyard")).thenReturn(3);
        de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition shipyardDefinition =
                new de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition("shipyard", "Shipyard", "desc",
                        new ResourceCost(200, 100, 0), 1.15, 60, de.kugi.dev.battleoftheuniverse.catalog.ResourceKey.NONE, 0, 5, List.of());
        when(catalogService.building("shipyard")).thenReturn(shipyardDefinition);

        assertThatThrownBy(() -> service.queueShip(ORIGIN_ID, OWNER_ID, "galaxy_class", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Requirements not met");
        verify(resourceService, never()).debit(any(), any(ResourceCost.class));
    }
}
