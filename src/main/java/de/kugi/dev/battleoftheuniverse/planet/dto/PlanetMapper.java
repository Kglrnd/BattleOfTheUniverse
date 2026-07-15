package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.Planet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanetMapper {

    /** Number of planet image assets the frontend ships (frontend/public/images/planets/0.webp .. {N-1}.webp). */
    int PLANET_IMAGE_VARIANT_COUNT = 6;

    @Mapping(
            target = "imageVariant",
            expression = "java(planet.getId() == null ? 0 : "
                    + "(int) Math.floorMod(planet.getId() * 2654435761L, (long) PLANET_IMAGE_VARIANT_COUNT))"
    )
    PlanetView toView(Planet planet);

    AdminPlanetView toAdminView(Planet planet, String ownerUsername);
}
