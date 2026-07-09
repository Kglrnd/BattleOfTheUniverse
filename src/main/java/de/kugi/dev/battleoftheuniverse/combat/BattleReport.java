package de.kugi.dev.battleoftheuniverse.combat;

import java.time.Instant;
import java.util.Map;

/**
 * Published once an {@code AttackArrived} battle has been resolved; consumed by
 * {@code message} to notify both sides. Loss maps are ship/tower catalog key -> quantity
 * destroyed, and are empty (not absent) when a side took no losses.
 */
public record BattleReport(
        Long attackerId,
        Long defenderId,
        Long defenderPlanetId,
        String defenderPlanetName,
        Map<String, Integer> attackerLosses,
        Map<String, Integer> defenderTowerLosses,
        Map<String, Integer> defenderShipLosses,
        Instant occurredAt
) {
}
