package de.kugi.dev.battleoftheuniverse.fleet;

/** Published when a colonize mission successfully founds a new colony; consumed by {@code message} to notify the owner. */
public record ColonyFounded(Long ownerId, Long planetId, String planetName, double researchEfficiency) {
}
