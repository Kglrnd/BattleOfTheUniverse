package de.kugi.dev.battleoftheuniverse.fleet;

import java.util.Map;

/**
 * Published when an invade mission reaches its target; consumed by {@code combat} to resolve
 * any defense first, then roll the invasion unit's capture-planet effect and credit survivors
 * back to {@code attackerOriginPlanetId}.
 */
public record InvadeArrived(
        Long attackerId,
        Long attackerOriginPlanetId,
        Long defenderId,
        Long defenderPlanetId,
        String defenderPlanetName,
        Map<String, Integer> attackingShips
) {
}
