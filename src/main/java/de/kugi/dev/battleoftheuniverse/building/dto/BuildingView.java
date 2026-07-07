package de.kugi.dev.battleoftheuniverse.building.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;

public record BuildingView(
        String key,
        String name,
        String description,
        int level,
        ResourceCost nextLevelCost,
        long nextLevelBuildTimeSeconds,
        boolean constructionActive,
        Instant constructionEndsAt
) {
}
