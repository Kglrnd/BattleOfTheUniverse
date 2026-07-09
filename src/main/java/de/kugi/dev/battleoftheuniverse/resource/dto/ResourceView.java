package de.kugi.dev.battleoftheuniverse.resource.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;

public record ResourceView(ResourceKey resourceKey, String displayName, long amount) {
}
