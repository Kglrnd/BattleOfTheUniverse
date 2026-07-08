package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * {@code driveScope}/{@code baseSpeedMultiplier}/{@code driveSpeedBonusPerLevel} only apply
 * to propulsion technologies ("drives"); every other technology sets {@code driveScope} to
 * {@link DriveScope#NONE}, {@code baseSpeedMultiplier} to {@code 1.0}, and
 * {@code driveSpeedBonusPerLevel} to {@code 0}. Drives are otherwise researched exactly like
 * any other technology — same cost/level growth — so they share this one record rather than
 * a parallel catalog.
 * <p>
 * A drive's effective speed multiplier is {@code baseSpeedMultiplier + driveSpeedBonusPerLevel
 * * level}. Splitting the formula into a base and a per-level slope (rather than a single
 * {@code 1.0 + bonus * level}) lets two drives of the same scope trade off differently: one
 * with a higher base but shallower slope is faster early on, while one with a lower base but
 * steeper slope starts behind and overtakes it at high levels.
 */
public record TechnologyDefinition(
        String key,
        String name,
        String description,
        ResourceCost baseCost,
        double costGrowthFactor,
        int baseResearchTimeSeconds,
        DriveScope driveScope,
        double baseSpeedMultiplier,
        double driveSpeedBonusPerLevel
) {
}
