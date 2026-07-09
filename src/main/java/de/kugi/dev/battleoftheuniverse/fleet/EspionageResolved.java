package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceView;

import java.util.List;

/**
 * Published when an espionage mission resolves; consumed by {@code message} to notify the
 * attacker (report or failure notice) and, on failure, the defender. {@code stationedShips}
 * and {@code resources} are empty when {@code success} is {@code false}.
 */
public record EspionageResolved(
        Long attackerId,
        Long defenderId,
        Long targetPlanetId,
        String targetPlanetName,
        boolean success,
        List<ShipQuantity> stationedShips,
        List<ResourceView> resources
) {
}
