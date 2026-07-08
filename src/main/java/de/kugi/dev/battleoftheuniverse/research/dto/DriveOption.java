package de.kugi.dev.battleoftheuniverse.research.dto;

import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;

/** A researched drive capable of a given mission range, with its current speed multiplier. */
public record DriveOption(
        String key,
        String name,
        DriveScope driveScope,
        int level,
        double speedMultiplier
) {
}
