package de.kugi.dev.battleoftheuniverse.catalog;

public record TechnologyDefinition(
        String key,
        String name,
        String description,
        ResourceCost baseCost,
        double costGrowthFactor,
        int baseResearchTimeSeconds
) {
}
