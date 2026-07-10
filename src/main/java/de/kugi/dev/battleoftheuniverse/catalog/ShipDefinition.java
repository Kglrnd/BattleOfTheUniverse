package de.kugi.dev.battleoftheuniverse.catalog;

public record ShipDefinition(
        String key,
        String name,
        String description,
        int attack,
        int defense,
        int speed,
        int cargoCapacity,
        int hydrogenConsumption,
        ResourceCost baseCost,
        int baseBuildTimeSeconds,
        int points
) {
}
