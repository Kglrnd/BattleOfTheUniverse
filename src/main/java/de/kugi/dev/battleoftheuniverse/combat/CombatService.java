package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.AttackArrived;
import de.kugi.dev.battleoftheuniverse.fleet.BombardArrived;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import de.kugi.dev.battleoftheuniverse.fleet.InvadeArrived;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.PlanetResource;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Resolves an {@link AttackArrived} battle: the attacker's total attack power and the
 * defender's total defense power (towers, then the stationed fleet) are compared, and
 * both sides lose a share of their forces proportional to the opposing side's power
 * relative to their own - towers absorb damage before the defender's fleet does. Weapons
 * research boosts attack power, shields research boosts both towers' and the stationed
 * fleet's defense power. A decisive win (boosted attack power exceeding boosted defense
 * power) loots resources from the defender, capped by the attacker's cargo capacity.
 * {@link BombardArrived}/{@link InvadeArrived} reuse the same battle math (see
 * {@link #resolveCombat}) - an undefended target skips combat entirely, a defended one
 * fights first, and the special ship only gets its ~95% success roll if it survives.
 */
@Component
@RequiredArgsConstructor
public class CombatService {

    /** The technology that currently boosts attack power - matches the "laser_technology" catalog entry. */
    private static final String WEAPONS_TECH_KEY = "laser_technology";
    private static final String SHIELDS_TECH_KEY = "shielding_technology";
    private static final double WEAPONS_BONUS_PER_LEVEL = 0.1;
    private static final double SHIELDS_BONUS_PER_LEVEL = 0.1;
    private static final double LOOT_FRACTION = 0.5;

    /** Matches the "orbital_bomb" catalog entry. */
    private static final String BOMB_KEY = "orbital_bomb";

    /** Matches the "invasion_unit" catalog entry. */
    private static final String INVASION_KEY = "invasion_unit";

    /** Chance a BOMBARD/INVADE succeeds once its special ship survives to the target. */
    private static final double SPECIAL_MISSION_SUCCESS_CHANCE = 0.95;

    private final FleetService fleetService;
    private final DefenseService defenseService;
    private final CatalogService catalogService;
    private final ResearchService researchService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final BuildingService buildingService;
    private final ApplicationEventPublisher events;

    @ApplicationModuleListener
    void on(AttackArrived event) {
        Map<String, Integer> towers = defenseService.stationedTowers(event.defenderPlanetId());
        Map<String, Integer> defenderShips = fleetService.stationedShips(event.defenderPlanetId());
        BattleOutcome outcome = resolveCombat(event.attackingShips(), event.attackerId(), event.defenderId(),
                event.defenderPlanetId(), towers, defenderShips);
        fleetService.creditShips(event.attackerOriginPlanetId(), outcome.survivors());

        ResourceCost loot = ResourceCost.ZERO;
        if (outcome.decisive()) {
            loot = computeLoot(event.defenderPlanetId(), event.attackingShips());
            if (loot.metal() > 0 || loot.crystal() > 0 || loot.deuterium() > 0) {
                resourceService.debit(event.defenderPlanetId(), loot);
                resourceService.credit(event.attackerOriginPlanetId(), loot);
            }
        }

        events.publishEvent(new BattleReport(event.attackerId(), event.defenderId(), event.defenderPlanetId(),
                event.defenderPlanetName(), event.attackingShips(), outcome.attackerLosses(), outcome.towers(),
                outcome.towerLosses(), outcome.defenderShips(), outcome.shipLosses(), loot, Instant.now()));
    }

    @ApplicationModuleListener
    void on(BombardArrived event) {
        resolveSpecialMission(event.attackerId(), event.attackerOriginPlanetId(), event.defenderId(),
                event.defenderPlanetId(), event.defenderPlanetName(), event.attackingShips(), BOMB_KEY, () -> {
                    String coordinates = planetService.getById(event.defenderPlanetId()).getCoordinates();
                    planetService.destroyPlanet(event.defenderPlanetId());
                    buildingService.deleteAllForPlanet(event.defenderPlanetId());
                    resourceService.deleteAllForPlanet(event.defenderPlanetId());
                    fleetService.wipeAllShipsAndOrders(event.defenderPlanetId());
                    defenseService.deleteAllForPlanet(event.defenderPlanetId());
                    events.publishEvent(new PlanetDestroyed(event.attackerId(), event.defenderId(),
                            event.defenderPlanetId(), event.defenderPlanetName(), coordinates, Instant.now()));
                });
    }

    @ApplicationModuleListener
    void on(InvadeArrived event) {
        resolveSpecialMission(event.attackerId(), event.attackerOriginPlanetId(), event.defenderId(),
                event.defenderPlanetId(), event.defenderPlanetName(), event.attackingShips(), INVASION_KEY, () -> {
                    planetService.reassignOwner(event.defenderPlanetId(), event.attackerId());
                    events.publishEvent(new PlanetInvaded(event.attackerId(), event.defenderId(),
                            event.defenderPlanetId(), event.defenderPlanetName(), Instant.now()));
                });
    }

    /**
     * Shared BOMBARD/INVADE resolution: an undefended target skips combat entirely (the special
     * ship arrives at full strength, no report - there was nothing to fight); a defended target
     * fights a normal battle first via {@link #resolveCombat} - and, just like ATTACK, publishes
     * a {@link BattleReport} so both sides see what happened even if the special ship doesn't
     * survive - and the special ship (and everything else) can be lost there like any other unit.
     * Either way, the effect only fires if at least one special ship survives to the target *and*
     * a {@value #SPECIAL_MISSION_SUCCESS_CHANCE}-chance roll succeeds - all special ships are
     * consumed by a successful effect, mirroring how COLONIZE consumes every colony ship in the
     * fleet. On failure (or no eligible survivor), the whole surviving fleet just returns home.
     */
    private void resolveSpecialMission(Long attackerId, Long attackerOriginPlanetId, Long defenderId, Long defenderPlanetId,
                                        String defenderPlanetName, Map<String, Integer> attackingShips, String specialShipKey,
                                        Runnable applyEffect) {
        Map<String, Integer> towers = defenseService.stationedTowers(defenderPlanetId);
        Map<String, Integer> defenderShips = fleetService.stationedShips(defenderPlanetId);

        Map<String, Integer> survivors;
        if (towers.isEmpty() && defenderShips.isEmpty()) {
            survivors = new HashMap<>(attackingShips);
        } else {
            BattleOutcome outcome = resolveCombat(attackingShips, attackerId, defenderId, defenderPlanetId, towers, defenderShips);
            survivors = outcome.survivors();
            events.publishEvent(new BattleReport(attackerId, defenderId, defenderPlanetId, defenderPlanetName,
                    attackingShips, outcome.attackerLosses(), outcome.towers(), outcome.towerLosses(),
                    outcome.defenderShips(), outcome.shipLosses(), ResourceCost.ZERO, Instant.now()));
        }

        boolean effectEligible = survivors.getOrDefault(specialShipKey, 0) > 0;
        if (effectEligible && ThreadLocalRandom.current().nextDouble() < SPECIAL_MISSION_SUCCESS_CHANCE) {
            Map<String, Integer> returning = new HashMap<>(survivors);
            returning.remove(specialShipKey);
            fleetService.creditShips(attackerOriginPlanetId, returning);
            applyEffect.run();
        } else {
            fleetService.creditShips(attackerOriginPlanetId, survivors);
        }
    }

    /**
     * The full power/loss math shared by every mission type that ends in combat. Applies losses
     * to the defender's towers/fleet as a side effect (unconditionally correct regardless of
     * caller); the caller decides what to do with survivors (credit them back, consume some of
     * them for a special effect, etc).
     */
    private BattleOutcome resolveCombat(Map<String, Integer> attackingShips, Long attackerId, Long defenderId, Long defenderPlanetId,
                                         Map<String, Integer> towers, Map<String, Integer> defenderShips) {
        long attackPower = totalPower(attackingShips, key -> catalogService.ship(key).attack());
        attackPower = Math.round(attackPower * (1 + WEAPONS_BONUS_PER_LEVEL * researchService.levelOf(attackerId, WEAPONS_TECH_KEY)));

        long towerPower = totalPower(towers, key -> catalogService.defense(key).defense());
        long fleetPower = totalPower(defenderShips, key -> catalogService.ship(key).defense());
        double shieldMultiplier = 1 + SHIELDS_BONUS_PER_LEVEL * researchService.levelOf(defenderId, SHIELDS_TECH_KEY);
        towerPower = Math.round(towerPower * shieldMultiplier);
        fleetPower = Math.round(fleetPower * shieldMultiplier);

        long powerTraded = Math.min(attackPower, towerPower + fleetPower);
        long towerDamage = Math.min(powerTraded, towerPower);
        long fleetDamage = powerTraded - towerDamage;

        Map<String, Integer> towerLosses = distributeLosses(towers, towerPower, towerDamage);
        Map<String, Integer> shipLosses = distributeLosses(defenderShips, fleetPower, fleetDamage);
        Map<String, Integer> attackerLosses = distributeLosses(attackingShips, attackPower, powerTraded);

        if (!towerLosses.isEmpty()) {
            defenseService.applyLosses(defenderPlanetId, towerLosses);
        }
        if (!shipLosses.isEmpty()) {
            fleetService.applyLosses(defenderPlanetId, shipLosses);
        }

        Map<String, Integer> survivors = new HashMap<>();
        attackingShips.forEach((key, qty) -> {
            int remaining = qty - attackerLosses.getOrDefault(key, 0);
            if (remaining > 0) {
                survivors.put(key, remaining);
            }
        });

        return new BattleOutcome(towers, towerLosses, defenderShips, shipLosses, attackerLosses, survivors, attackPower, towerPower + fleetPower);
    }

    private record BattleOutcome(
            Map<String, Integer> towers,
            Map<String, Integer> towerLosses,
            Map<String, Integer> defenderShips,
            Map<String, Integer> shipLosses,
            Map<String, Integer> attackerLosses,
            Map<String, Integer> survivors,
            long attackPower,
            long defensePower
    ) {
        boolean decisive() {
            return attackPower > defensePower;
        }
    }

    private ResourceCost computeLoot(Long defenderPlanetId, Map<String, Integer> attackingShips) {
        Map<ResourceKey, Long> onHand = resourceService.raw(defenderPlanetId).stream()
                .collect(Collectors.toMap(PlanetResource::getResourceKey, PlanetResource::getAmount));
        long metal = Math.round(onHand.getOrDefault(ResourceKey.METAL, 0L) * LOOT_FRACTION);
        long crystal = Math.round(onHand.getOrDefault(ResourceKey.CRYSTAL, 0L) * LOOT_FRACTION);
        long deuterium = Math.round(onHand.getOrDefault(ResourceKey.DEUTERIUM, 0L) * LOOT_FRACTION);
        long totalPotential = metal + crystal + deuterium;
        if (totalPotential <= 0) {
            return ResourceCost.ZERO;
        }

        long cargoCapacity = attackingShips.entrySet().stream()
                .mapToLong(e -> (long) catalogService.ship(e.getKey()).cargoCapacity() * e.getValue())
                .sum();
        if (totalPotential <= cargoCapacity) {
            return new ResourceCost(metal, crystal, deuterium);
        }

        double scale = (double) cargoCapacity / totalPotential;
        return new ResourceCost(Math.round(metal * scale), Math.round(crystal * scale), Math.round(deuterium * scale));
    }

    private long totalPower(Map<String, Integer> owned, ToIntFunction<String> statFor) {
        return owned.entrySet().stream()
                .mapToLong(e -> (long) statFor.applyAsInt(e.getKey()) * e.getValue())
                .sum();
    }

    /**
     * Splits a destroyed-power budget across unit types proportional to each type's share
     * of the group's owned quantity (every type in the group takes the same loss fraction) -
     * never destroys more than is owned. Rounded to the nearest whole unit rather than floored:
     * flooring outright meant any stack small enough that {@code qty * fraction < 1} (e.g. 9
     * ships at a 10% loss fraction, i.e. 0.9 destroyed) always took zero losses no matter how
     * lopsided the battle - rounding still under-counts very small fractions (below 0.5) but at
     * least stops guaranteeing full immortality for a fraction that rounds up.
     */
    private Map<String, Integer> distributeLosses(Map<String, Integer> owned, long totalPower, long destroyedPower) {
        if (destroyedPower <= 0 || totalPower <= 0) {
            return Map.of();
        }
        double fraction = Math.min(1.0, (double) destroyedPower / totalPower);
        Map<String, Integer> losses = new HashMap<>();
        owned.forEach((key, qty) -> {
            int lost = (int) Math.round(qty * fraction);
            if (lost > 0) {
                losses.put(key, lost);
            }
        });
        return losses;
    }
}
