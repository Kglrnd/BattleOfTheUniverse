package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;

import java.time.Instant;

public record AdminPlanetView(
        Long id,
        String name,
        Long ownerId,
        String ownerUsername,
        int galaxy,
        int system,
        int position,
        String coordinates,
        PlanetClass planetClass,
        boolean homeworld,
        boolean destroyed,
        Instant createdAt
) {
}
