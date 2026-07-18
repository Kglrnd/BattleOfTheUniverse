package de.kugi.dev.battleoftheuniverse.fleet.dto;

import java.time.Instant;

/** One slot in the shipyard's build pipeline - see {@link ShipyardQueueView}. */
public record ShipyardQueueEntryView(
        String shipKey,
        String shipName,
        int quantity,
        int position,
        Instant startedAt,
        Instant endsAt
) {
}
