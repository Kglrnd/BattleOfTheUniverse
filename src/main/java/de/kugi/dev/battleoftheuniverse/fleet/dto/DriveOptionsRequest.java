package de.kugi.dev.battleoftheuniverse.fleet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DriveOptionsRequest(
        @NotNull Long originPlanetId,
        @NotEmpty List<@Valid ShipQuantity> ships,
        @Min(1) int targetGalaxy,
        @Min(1) int targetSystem,
        @Min(1) int targetPosition
) {
}
