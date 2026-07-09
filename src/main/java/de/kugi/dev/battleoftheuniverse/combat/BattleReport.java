package de.kugi.dev.battleoftheuniverse.combat;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;
import java.util.Map;

/**
 * Published once an {@code AttackArrived} battle has been resolved; consumed by
 * {@code message} to notify both sides. Loss maps and the composition maps are ship/tower
 * catalog key -> quantity, and are empty (not absent) when a side took no losses / fielded
 * nothing. {@code resourcesLooted} is {@link ResourceCost#ZERO} when nothing was looted.
 */
public record BattleReport(
        Long attackerId,
        Long defenderId,
        Long defenderPlanetId,
        String defenderPlanetName,
        Map<String, Integer> attackerShips,
        Map<String, Integer> attackerLosses,
        Map<String, Integer> defenderTowers,
        Map<String, Integer> defenderTowerLosses,
        Map<String, Integer> defenderShips,
        Map<String, Integer> defenderShipLosses,
        ResourceCost resourcesLooted,
        Instant occurredAt
) {
}
