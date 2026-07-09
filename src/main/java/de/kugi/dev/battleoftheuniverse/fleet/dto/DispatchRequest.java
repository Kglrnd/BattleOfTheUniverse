package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DispatchRequest(
        @NotNull Long originPlanetId,
        @NotEmpty List<@Valid ShipQuantity> ships,
        @NotNull FleetMissionType missionType,
        @Min(1) int targetGalaxy,
        @Min(1) int targetSystem,
        @Min(1) int targetPosition,
        @NotBlank String driveKey,
        List<@Valid ResourceQuantity> cargo
) {
    public DispatchRequest {
        cargo = cargo == null ? List.of() : cargo;
    }

    /** Convenience overload for callers that never send cargo (every mission but TRANSPORT). */
    public DispatchRequest(Long originPlanetId, List<ShipQuantity> ships, FleetMissionType missionType,
                            int targetGalaxy, int targetSystem, int targetPosition, String driveKey) {
        this(originPlanetId, ships, missionType, targetGalaxy, targetSystem, targetPosition, driveKey, List.of());
    }
}
