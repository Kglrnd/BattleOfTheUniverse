package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.defense.DefenseService;
import de.kugi.dev.battleoftheuniverse.fleet.AttackArrived;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an {@link AttackArrived} battle: the attacker's total attack power and the
 * defender's total defense power (towers, then the stationed fleet) are compared, and
 * both sides lose a share of their forces proportional to the opposing side's power
 * relative to their own - towers absorb damage before the defender's fleet does. See the
 * feature plan for the exact formula. No resource looting.
 */
@Component
@RequiredArgsConstructor
public class CombatService {

    private final FleetService fleetService;
    private final DefenseService defenseService;
    private final CatalogService catalogService;
    private final ApplicationEventPublisher events;

    @ApplicationModuleListener
    void on(AttackArrived event) {
        Map<String, Integer> attackingShips = event.attackingShips();
        long attackPower = totalPower(attackingShips, key -> catalogService.ship(key).attack());

        Map<String, Integer> towers = defenseService.stationedTowers(event.defenderPlanetId());
        Map<String, Integer> defenderShips = fleetService.stationedShips(event.defenderPlanetId());
        long towerPower = totalPower(towers, key -> catalogService.defense(key).defense());
        long fleetPower = totalPower(defenderShips, key -> catalogService.ship(key).defense());

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

        events.publishEvent(new BattleReport(event.attackerId(), event.defenderId(), event.defenderPlanetId(),
                event.defenderPlanetName(), attackerLosses, towerLosses, shipLosses, Instant.now()));
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
