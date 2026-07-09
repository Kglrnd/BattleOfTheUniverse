package de.kugi.dev.battleoftheuniverse.catalog;

import java.util.List;

/**
 * Static game-balance definition of a defense tower type. Flat per-unit cost/build-time
 * like {@link ShipDefinition} (towers are purchased in quantity, not leveled), but gated
 * by {@link Requirement}s like a building or technology - typically the defense-facility
 * building level.
 */
public record DefenseDefinition(
        String key,
        String name,
        String description,
        int defense,
        ResourceCost baseCost,
        int baseBuildTimeSeconds,
        List<Requirement> requirements
) {
}
