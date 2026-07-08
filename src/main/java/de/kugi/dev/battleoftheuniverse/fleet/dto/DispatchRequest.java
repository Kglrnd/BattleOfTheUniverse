package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DispatchRequest(
        @NotNull Long originPlanetId,
        @NotBlank String shipKey,
        @Positive int quantity,
        @NotNull FleetMissionType missionType,
        @Min(1) int targetGalaxy,
        @Min(1) int targetSystem,
        @Min(1) int targetPosition,
        @NotBlank String driveKey
) {
}
