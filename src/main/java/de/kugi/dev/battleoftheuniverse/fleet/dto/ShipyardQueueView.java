package de.kugi.dev.battleoftheuniverse.fleet.dto;

import java.util.List;

/**
 * A planet's shipyard build pipeline. {@code maxSize} is 0 (with no entries) below the shipyard
 * level that unlocks it, so a client can tell "not unlocked yet" apart from "unlocked but empty".
 */
public record ShipyardQueueView(
        int maxSize,
        List<ShipyardQueueEntryView> entries
) {
}
