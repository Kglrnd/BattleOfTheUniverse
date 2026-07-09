package de.kugi.dev.battleoftheuniverse.building.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;
import java.util.List;

public record BuildingView(
        String key,
        String name,
        String description,
        int level,
        ResourceCost nextLevelCost,
        long nextLevelBuildTimeSeconds,
        boolean constructionActive,
        Instant constructionEndsAt,
        boolean unlocked,
        List<LockedRequirement> missingRequirements
) {
}
