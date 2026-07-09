package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.AttackArrived;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
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
import java.util.stream.Collectors;

/**
 * Resolves an {@link AttackArrived} battle: the attacker's total attack power and the
 * defender's total defense power (towers, then the stationed fleet) are compared, and
 * both sides lose a share of their forces proportional to the opposing side's power
 * relative to their own - towers absorb damage before the defender's fleet does. Weapons
 * research boosts attack power, shields research boosts both towers' and the stationed
 * fleet's defense power. A decisive win (boosted attack power exceeding boosted defense
 * power) loots resources from the defender, capped by the attacker's cargo capacity. See
 * the feature plan for the exact formula.
 */
@Component
@RequiredArgsConstructor
public class CombatService {

    private static final String WEAPONS_TECH_KEY = "laser_technology";
    private static final String SHIELDS_TECH_KEY = "shielding_technology";
    private static final double WEAPONS_BONUS_PER_LEVEL = 0.1;
    private static final double SHIELDS_BONUS_PER_LEVEL = 0.1;
    private static final double LOOT_FRACTION = 0.5;

    private final FleetService fleetService;
    private final DefenseService defenseService;
    private final CatalogService catalogService;
    private final ResearchService researchService;
    private final ResourceService resourceService;
    private final ApplicationEventPublisher events;

    @ApplicationModuleListener
    void on(AttackArrived event) {
        Map<String, Integer> attackingShips = event.attackingShips();
        long attackPower = totalPower(attackingShips, key -> catalogService.ship(key).attack());
        attackPower = Math.round(attackPower * (1 + WEAPONS_BONUS_PER_LEVEL * researchService.levelOf(event.attackerId(), WEAPONS_TECH_KEY)));

        Map<String, Integer> towers = defenseService.stationedTowers(event.defenderPlanetId());
        Map<String, Integer> defenderShips = fleetService.stationedShips(event.defenderPlanetId());
        long towerPower = totalPower(towers, key -> catalogService.defense(key).defense());
        long fleetPower = totalPower(defenderShips, key -> catalogService.ship(key).defense());
        double shieldMultiplier = 1 + SHIELDS_BONUS_PER_LEVEL * researchService.levelOf(event.defenderId(), SHIELDS_TECH_KEY);
        towerPower = Math.round(towerPower * shieldMultiplier);
        fleetPower = Math.round(fleetPower * shieldMultiplier);

        long powerTraded = Math.min(attackPower, towerPower + fleetPower);
        long towerDamage = Math.min(powerTraded, towerPower);
        long fleetDamage = powerTraded - towerDamage;

        Map<String, Integer> towerLosses = distributeLosses(towers, towerPower, towerDamage);
        Map<String, Integer> shipLosses = distributeLosses(defenderShips, fleetPower, fleetDamage);
        Map<String, Integer> attackerLosses = distributeLosses(attackingShips, attackPower, powerTraded);

        if (!towerLosses.isEmpty()) {
            defenseService.applyLosses(event.defenderPlanetId(), towerLosses);
        }
        if (!shipLosses.isEmpty()) {
            fleetService.applyLosses(event.defenderPlanetId(), shipLosses);
        }

        Map<String, Integer> survivors = new HashMap<>();
        attackingShips.forEach((key, qty) -> {
            int remaining = qty - attackerLosses.getOrDefault(key, 0);
            if (remaining > 0) {
                survivors.put(key, remaining);
            }
        });
        fleetService.creditShips(event.attackerOriginPlanetId(), survivors);

        ResourceCost loot = ResourceCost.ZERO;
        if (attackPower > towerPower + fleetPower) {
            loot = computeLoot(event.defenderPlanetId(), attackingShips);
            if (loot.metal() > 0 || loot.crystal() > 0 || loot.deuterium() > 0) {
                resourceService.debit(event.defenderPlanetId(), loot);
                resourceService.credit(event.attackerOriginPlanetId(), loot);
            }
        }

        events.publishEvent(new BattleReport(event.attackerId(), event.defenderId(), event.defenderPlanetId(),
                event.defenderPlanetName(), attackingShips, attackerLosses, towers, towerLosses, defenderShips,
                shipLosses, loot, Instant.now()));
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

    private long totalPower(Map<String, Integer> owned, java.util.function.ToIntFunction<String> statFor) {
        return owned.entrySet().stream()
                .mapToLong(e -> (long) statFor.applyAsInt(e.getKey()) * e.getValue())
                .sum();
    }

    /**
     * Splits a destroyed-power budget across unit types proportional to each type's share
     * of the group's owned quantity (every type in the group takes the same loss fraction),
     * floored to whole units - never destroys more than is owned.
     */
    private Map<String, Integer> distributeLosses(Map<String, Integer> owned, long totalPower, long destroyedPower) {
        if (destroyedPower <= 0 || totalPower <= 0) {
            return Map.of();
        }
        double fraction = Math.min(1.0, (double) destroyedPower / totalPower);
        Map<String, Integer> losses = new HashMap<>();
        owned.forEach((key, qty) -> {
            int lost = (int) Math.floor(qty * fraction);
            if (lost > 0) {
                losses.put(key, lost);
            }
        });
        return losses;
    }
}
