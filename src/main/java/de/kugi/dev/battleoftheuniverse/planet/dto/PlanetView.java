package de.kugi.dev.battleoftheuniverse.planet.dto;

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
}
