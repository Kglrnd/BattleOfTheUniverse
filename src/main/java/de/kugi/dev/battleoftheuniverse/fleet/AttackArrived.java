package de.kugi.dev.battleoftheuniverse.fleet;

import java.util.Map;

/**
 * Published when an attack mission reaches its target; consumed by {@code combat} to
 * resolve the battle (towers, then the defender's stationed fleet, then the attacker's
 * own losses) and credit survivors back to {@code attackerOriginPlanetId}.
 */
public record AttackArrived(
        Long attackerId,
        Long attackerOriginPlanetId,
        Long defenderId,
        Long defenderPlanetId,
        String defenderPlanetName,
        Map<String, Integer> attackingShips
) {
}
