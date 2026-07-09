package de.kugi.dev.battleoftheuniverse.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** One ship type and how many of it are in a fleet's manifest. */
public record ShipQuantity(
        @NotBlank String shipKey,
        @Positive int quantity
) {
}
