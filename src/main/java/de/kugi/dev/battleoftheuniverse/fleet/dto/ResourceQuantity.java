package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One resource type and how much of it is loaded as cargo in a fleet's manifest. */
public record ResourceQuantity(
        @NotNull ResourceKey resourceKey,
        @Positive long amount
) {
}
