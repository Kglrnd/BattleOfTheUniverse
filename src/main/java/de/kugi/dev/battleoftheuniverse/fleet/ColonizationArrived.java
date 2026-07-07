package de.kugi.dev.battleoftheuniverse.fleet;

/**
 * Published when a COLONIZE mission's travel time elapses. No listener yet — the
 * planet module will react to this once colonization (turning the arrival into an
 * actual new Planet) is implemented.
 */
public record ColonizationArrived(
        Long ownerId,
        String shipKey,
        int quantity,
        int targetGalaxy,
        int targetSystem,
        int targetPosition
) {
}
