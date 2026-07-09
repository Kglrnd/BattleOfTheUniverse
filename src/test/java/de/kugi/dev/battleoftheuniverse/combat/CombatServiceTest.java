package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DefenseDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.AttackArrived;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatServiceTest {

    private static final Long ATTACKER_ID = 1L;
    private static final Long ORIGIN_ID = 10L;
    private static final Long DEFENDER_ID = 2L;
    private static final Long TARGET_PLANET_ID = 20L;

    @Mock
    private FleetService fleetService;
    @Mock
    private DefenseService defenseService;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ApplicationEventPublisher events;

    private CombatService service;

    private final ShipDefinition cruiser = new ShipDefinition(
            "cruiser", "Cruiser", "desc", 400, 200, 15000, 50, new ResourceCost(20000, 7000, 2000), 1800);
    private final ShipDefinition lightFighter = new ShipDefinition(
            "light_fighter", "Light Fighter", "desc", 50, 10, 12500, 50, new ResourceCost(3000, 1000, 0), 60);
    private final DefenseDefinition lightTower = new DefenseDefinition(
            "light_defense_tower", "Light Defense Tower", "desc", 50, new ResourceCost(2000, 500, 0), 300, List.of());

    @BeforeEach
    void setUp() {
        service = new CombatService(fleetService, defenseService, catalogService, events);
        lenient().when(catalogService.ship("cruiser")).thenReturn(cruiser);
        lenient().when(catalogService.ship("light_fighter")).thenReturn(lightFighter);
        lenient().when(catalogService.defense("light_defense_tower")).thenReturn(lightTower);
    }

    @Test
    void attackerMuchStrongerWipesTheDefenderWhileTakingProportionalLosses() {
        // attackPower = 10 cruisers * 400 = 4000; defensePower = 5 towers * 50 = 250 (no fleet)
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 5));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 10)));

        verify(defenseService).applyLosses(TARGET_PLANET_ID, Map.of("light_defense_tower", 5));
        verify(fleetService, never()).applyLosses(any(), any());

        // powerTraded = min(4000, 250) = 250; attackerLossFraction = 250/4000 = 0.0625 -> floor(10*0.0625) = 0
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> survivorsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fleetService).creditShips(eq(ORIGIN_ID), survivorsCaptor.capture());
        assertThat(survivorsCaptor.getValue()).containsExactlyEntriesOf(Map.of("cruiser", 10));

        ArgumentCaptor<BattleReport> reportCaptor = ArgumentCaptor.forClass(BattleReport.class);
        verify(events).publishEvent(reportCaptor.capture());
        assertThat(reportCaptor.getValue().attackerLosses()).isEmpty();
        assertThat(reportCaptor.getValue().defenderTowerLosses()).containsExactlyEntriesOf(Map.of("light_defense_tower", 5));
    }

    @Test
    void defenderMuchStrongerInflictsHeavyLossesOnTheAttacker() {
        // attackPower = 1 cruiser * 400 = 400; defensePower = 20 towers * 50 = 1000
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 20));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 1)));

        // powerTraded = min(400, 1000) = 400; towerLossFraction = 400/1000 = 0.4 -> floor(20*0.4) = 8
        verify(defenseService).applyLosses(TARGET_PLANET_ID, Map.of("light_defense_tower", 8));
        // attackerLossFraction = 400/400 = 1.0 -> the single cruiser is destroyed
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> survivorsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fleetService).creditShips(eq(ORIGIN_ID), survivorsCaptor.capture());
        assertThat(survivorsCaptor.getValue()).isEmpty();
    }

    @Test
    void towersAbsorbDamageBeforeTheStationedFleetTakesAnyLosses() {
        // attackPower = 1 cruiser * 400 = 400; towerPower = 10 towers * 50 = 500 (already exceeds attackPower)
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 10));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of("light_fighter", 100));

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 1)));

        // All 400 power is spent on towers (towerPower=500 > 400), so the fleet is never touched.
        verify(defenseService).applyLosses(TARGET_PLANET_ID, Map.of("light_defense_tower", 8));
        verify(fleetService, never()).applyLosses(any(), any());
    }

    @Test
    void undefendedPlanetTakesNoLossesAndInflictsNoneBack() {
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of());
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 3)));

        verify(defenseService, never()).applyLosses(any(), any());
        verify(fleetService, never()).applyLosses(any(), any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> survivorsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fleetService).creditShips(eq(ORIGIN_ID), survivorsCaptor.capture());
        assertThat(survivorsCaptor.getValue()).containsExactlyEntriesOf(Map.of("cruiser", 3));
    }

    @Test
    void aFleetWithNoAttackPowerIsANoOp() {
        // colony_ship-only "attack" (attack stat 0) - shouldn't normally happen but must degrade gracefully.
        ShipDefinition colonyShip = new ShipDefinition(
                "colony_ship", "Colony Ship", "desc", 0, 100, 2500, 7500, new ResourceCost(10000, 20000, 10000), 3600);
        when(catalogService.ship("colony_ship")).thenReturn(colonyShip);
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 5));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("colony_ship", 1)));

        verify(defenseService, never()).applyLosses(any(), any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> survivorsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fleetService).creditShips(eq(ORIGIN_ID), survivorsCaptor.capture());
        assertThat(survivorsCaptor.getValue()).containsExactlyEntriesOf(Map.of("colony_ship", 1));
    }
}
