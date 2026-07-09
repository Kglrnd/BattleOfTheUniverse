package de.kugi.dev.battleoftheuniverse.defense.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;
import java.util.List;

public record TowerView(
        String key,
        String name,
        String description,
        int owned,
        int defense,
        ResourceCost unitCost,
        long unitBuildTimeSeconds,
        boolean buildActive,
        Integer buildingQuantity,
        Instant buildEndsAt,
        boolean unlocked,
        List<LockedRequirement> missingRequirements
) {
}
