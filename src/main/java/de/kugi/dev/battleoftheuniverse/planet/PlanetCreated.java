package de.kugi.dev.battleoftheuniverse.planet;

/** Published after a planet is persisted; {@code resource} and {@code building} seed their starter state off this. */
public record PlanetCreated(Long planetId, Long ownerId) {
}
