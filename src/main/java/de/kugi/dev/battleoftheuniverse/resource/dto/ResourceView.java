package de.kugi.dev.battleoftheuniverse.resource.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.PlanetResource;

public record ResourceView(ResourceKey resourceKey, String displayName, long amount) {
    public static ResourceView from(PlanetResource resource) {
        return new ResourceView(resource.getResourceKey(), resource.getResourceKey().getDisplayName(), resource.getAmount());
    }
}
