package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * Static game-balance definition of a building type. Instances are data, not code —
 * they live in {@code catalog/buildings.json} and are editable by admins through the
 * JSON-Schema-driven catalog editor.
 */
public record BuildingDefinition(
        String key,
        String name,
        String description,
        ResourceCost baseCost,
        double costGrowthFactor,
        int baseBuildTimeSeconds,
        ResourceKey producesResource,
        double baseProductionPerLevel
) {
}
