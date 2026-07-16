package de.kugi.dev.battleoftheuniverse.combat;

import java.time.Instant;

/** Published once a successful invasion has transferred a planet to its attacker; consumed by {@code message}. */
public record PlanetInvaded(
        Long attackerId,
        Long defenderId,
        Long planetId,
        String planetName,
        Instant occurredAt
) {
}
