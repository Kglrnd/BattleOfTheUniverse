package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.Planet;
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
        Instant createdAt
) {
    public static AdminPlanetView from(Planet planet, String ownerUsername) {
        return new AdminPlanetView(
                planet.getId(),
                planet.getName(),
                planet.getOwnerId(),
                ownerUsername,
                planet.getGalaxy(),
                planet.getSystem(),
                planet.getPosition(),
                planet.getCoordinates(),
                planet.getPlanetClass(),
                planet.isHomeworld(),
                planet.getCreatedAt()
        );
    }
}
