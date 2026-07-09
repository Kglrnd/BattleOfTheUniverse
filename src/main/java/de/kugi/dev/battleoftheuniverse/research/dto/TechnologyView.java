package de.kugi.dev.battleoftheuniverse.research.dto;

import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;

import java.time.Instant;
import java.util.List;

public record TechnologyView(
        String key,
        String name,
        String description,
        DriveScope driveScope,
        int level,
        ResourceCost nextLevelCost,
        long nextLevelResearchTimeSeconds,
        boolean researchActive,
        Integer researchTargetLevel,
        Instant researchEndsAt,
        boolean unlocked,
        List<LockedRequirement> missingRequirements
) {
}
