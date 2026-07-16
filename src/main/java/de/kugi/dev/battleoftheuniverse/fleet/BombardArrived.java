package de.kugi.dev.battleoftheuniverse.fleet;

import java.util.Map;

/**
 * Published when a bombard mission reaches its target; consumed by {@code combat} to resolve
 * any defense first, then roll the orbital bomb's destroy-planet effect and credit survivors
 * back to {@code attackerOriginPlanetId}.
 */
public record BombardArrived(
        Long attackerId,
        Long attackerOriginPlanetId,
        Long defenderId,
        Long defenderPlanetId,
        String defenderPlanetName,
        Map<String, Integer> attackingShips
) {
}
