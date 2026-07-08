package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;

/** A researched drive the player could pick for this specific dispatch, with its resulting ETA. */
public record DriveOptionView(
        String key,
        String name,
        DriveScope driveScope,
        int level,
        double speedMultiplier,
        long etaSeconds
) {
}
