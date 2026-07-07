package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * {@code driveScope}/{@code driveSpeedBonusPerLevel} only apply to propulsion
 * technologies ("drives"); every other technology sets {@code driveScope} to
 * {@link DriveScope#NONE} and {@code driveSpeedBonusPerLevel} to {@code 0}. Drives
 * are otherwise researched exactly like any other technology — same cost/level
 * growth — so they share this one record rather than a parallel catalog.
 */
public record TechnologyDefinition(
        String key,
        String name,
        String description,
        ResourceCost baseCost,
        double costGrowthFactor,
        int baseResearchTimeSeconds,
        DriveScope driveScope,
        double driveSpeedBonusPerLevel
) {
}
