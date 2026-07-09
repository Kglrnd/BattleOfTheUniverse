package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;

public record ShipyardView(
        String key,
        String name,
        String description,
        int owned,
        int cargoCapacity,
        int hydrogenConsumption,
        ResourceCost unitCost,
        long unitBuildTimeSeconds,
        boolean buildActive,
        Integer buildingQuantity,
        Instant buildEndsAt
) {
}
