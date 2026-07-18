package de.kugi.dev.battleoftheuniverse.building.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;

import java.util.List;

/**
 * Production overview for one resource-producing building on a planet: its current output plus
 * up to 5 levels below and 5 levels above the current level (the "below" window is clipped at
 * level 0 - there's no upper level cap, so the "above" window is always full).
 */
public record ResourceProductionView(
        String buildingKey,
        String buildingName,
        String buildingDescription,
        ResourceKey resourceKey,
        String resourceDisplayName,
        int currentLevel,
        double productionEfficiency,
        double currentProductionPerHour,
        List<ProductionLevelView> levels
) {
}
