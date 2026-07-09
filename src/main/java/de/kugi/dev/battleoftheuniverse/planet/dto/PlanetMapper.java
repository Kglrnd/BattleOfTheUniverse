package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.Planet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanetMapper {

    PlanetView toView(Planet planet);

    AdminPlanetView toAdminView(Planet planet, String ownerUsername);
}
