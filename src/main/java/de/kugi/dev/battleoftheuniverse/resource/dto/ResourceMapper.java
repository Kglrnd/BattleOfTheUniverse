package de.kugi.dev.battleoftheuniverse.resource.dto;

import de.kugi.dev.battleoftheuniverse.resource.PlanetResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    @Mapping(target = "displayName", expression = "java(resource.getResourceKey().getDisplayName())")
    ResourceView toView(PlanetResource resource);
}
