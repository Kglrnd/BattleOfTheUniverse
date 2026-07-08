package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;

public record PlanetView(
        Long id,
        String name,
        int galaxy,
        int system,
        int position,
        String coordinates,
        PlanetClass planetClass,
        boolean homeworld
) {
    public static PlanetView from(Planet planet) {
        return new PlanetView(
                planet.getId(),
                planet.getName(),
                planet.getGalaxy(),
                planet.getSystem(),
                planet.getPosition(),
                planet.getCoordinates(),
                planet.getPlanetClass(),
                planet.isHomeworld()
        );
    }
}
