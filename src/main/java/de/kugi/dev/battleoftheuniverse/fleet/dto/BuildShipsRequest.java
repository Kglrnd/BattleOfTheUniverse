package de.kugi.dev.battleoftheuniverse.fleet.dto;

import jakarta.validation.constraints.Positive;

public record BuildShipsRequest(@Positive int quantity) {
}
