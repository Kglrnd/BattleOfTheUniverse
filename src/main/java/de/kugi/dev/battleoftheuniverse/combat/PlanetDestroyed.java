package de.kugi.dev.battleoftheuniverse.combat;

import java.time.Instant;

/** Published once a successful orbital bombardment has permanently destroyed a planet; consumed by {@code message}. */
public record PlanetDestroyed(
        Long attackerId,
        Long defenderId,
        Long planetId,
        String planetName,
        String planetCoordinates,
        Instant occurredAt
) {
}
