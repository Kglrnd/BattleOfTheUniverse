package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DefenseDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.AttackArrived;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.PlanetResource;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
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
    private ResearchService researchService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private ApplicationEventPublisher events;

    private CombatService service;

    private final ShipDefinition cruiser = new ShipDefinition(
            "cruiser", "Cruiser", "desc", 400, 200, 15000, 50, 4, new ResourceCost(20000, 7000, 2000), 1800, 29);
    private final ShipDefinition lightFighter = new ShipDefinition(
            "light_fighter", "Light Fighter", "desc", 50, 10, 12500, 50, 1, new ResourceCost(3000, 1000, 0), 60, 4);
    private final DefenseDefinition lightTower = new DefenseDefinition(
            "light_defense_tower", "Light Defense Tower", "desc", 50, new ResourceCost(2000, 500, 0), 300, 3, List.of());

    @BeforeEach
    void setUp() {
        service = new CombatService(fleetService, defenseService, catalogService, researchService, resourceService, events);
        lenient().when(catalogService.ship("cruiser")).thenReturn(cruiser);
        lenient().when(catalogService.ship("light_fighter")).thenReturn(lightFighter);
        lenient().when(catalogService.defense("light_defense_tower")).thenReturn(lightTower);
        lenient().when(researchService.levelOf(any(), any())).thenReturn(0);
        lenient().when(resourceService.raw(any())).thenReturn(List.of());
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
                "colony_ship", "Colony Ship", "desc", 0, 100, 2500, 7500, 6, new ResourceCost(10000, 20000, 10000), 3600, 0);
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

    @Test
    void weaponsResearchBoostsAttackPowerAndInflictsMoreTowerLosses() {
        // Same shape as defenderMuchStrongerInflictsHeavyLossesOnTheAttacker (towers=20, base attackPower=400,
        // unboosted losses would be 8) but the attacker has laser_technology level 5 -> x1.5 attack power = 600.
        when(researchService.levelOf(ATTACKER_ID, "laser_technology")).thenReturn(5);
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 20));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 1)));

        // attackPower = round(400*1.5) = 600; powerTraded = min(600,1000) = 600; fraction = 0.6 -> floor(20*0.6) = 12
        verify(defenseService).applyLosses(TARGET_PLANET_ID, Map.of("light_defense_tower", 12));
    }

    @Test
    void shieldsResearchBoostsDefensePowerAndReducesTowerLosses() {
        // Same shape as attackerMuchStrongerWipesTheDefenderWhileTakingProportionalLosses (attackPower=400,
        // towers=10, base towerPower=500) but the defender has shielding_technology level 5 -> x1.5 = 750.
        when(researchService.levelOf(DEFENDER_ID, "shielding_technology")).thenReturn(5);
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 10));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 1)));

        // towerPower = round(500*1.5) = 750; powerTraded = min(400,750) = 400; fraction = 400/750 -> floor(10*0.5333) = 5
        verify(defenseService).applyLosses(TARGET_PLANET_ID, Map.of("light_defense_tower", 5));
    }

    @Test
    void aDecisiveWinLootsResourcesCappedByCargoCapacity() {
        // 10 cruisers (attackPower=4000, cargoCapacity=10*50=500) vs 5 towers (towerPower=250) - a decisive win.
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 5));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());
        when(resourceService.raw(TARGET_PLANET_ID)).thenReturn(List.of(
                new PlanetResource(TARGET_PLANET_ID, ResourceKey.METAL, 1000),
                new PlanetResource(TARGET_PLANET_ID, ResourceKey.CRYSTAL, 0),
                new PlanetResource(TARGET_PLANET_ID, ResourceKey.DEUTERIUM, 0)));

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 10)));

        // 50% of 1000 metal = 500, which exactly fits the 500 cargo capacity - not scaled down.
        ResourceCost expectedLoot = new ResourceCost(500, 0, 0);
        verify(resourceService).debit(TARGET_PLANET_ID, expectedLoot);
        verify(resourceService).credit(ORIGIN_ID, expectedLoot);

        ArgumentCaptor<BattleReport> reportCaptor = ArgumentCaptor.forClass(BattleReport.class);
        verify(events).publishEvent(reportCaptor.capture());
        assertThat(reportCaptor.getValue().resourcesLooted()).isEqualTo(expectedLoot);
    }

    @Test
    void noLootingWhenTheAttackerDoesNotComeOutAhead() {
        // towerPower(500) >= attackPower(400) - not a decisive win, so nothing should be looted even though
        // the defender has resources on hand.
        when(defenseService.stationedTowers(TARGET_PLANET_ID)).thenReturn(Map.of("light_defense_tower", 10));
        when(fleetService.stationedShips(TARGET_PLANET_ID)).thenReturn(Map.of());
        lenient().when(resourceService.raw(TARGET_PLANET_ID)).thenReturn(List.of(
                new PlanetResource(TARGET_PLANET_ID, ResourceKey.METAL, 1000)));

        service.on(new AttackArrived(ATTACKER_ID, ORIGIN_ID, DEFENDER_ID, TARGET_PLANET_ID, "Target", Map.of("cruiser", 1)));

        verify(resourceService, never()).debit(any(), any());
        verify(resourceService, never()).credit(any(), any());

        ArgumentCaptor<BattleReport> reportCaptor = ArgumentCaptor.forClass(BattleReport.class);
        verify(events).publishEvent(reportCaptor.capture());
        assertThat(reportCaptor.getValue().resourcesLooted()).isEqualTo(ResourceCost.ZERO);
    }
}
