package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMissionType;

import java.time.Instant;
import java.util.List;

public record FleetMovementView(
        Long id,
        Long originPlanetId,
        List<ShipQuantity> ships,
        FleetMissionType missionType,
        int targetGalaxy,
        int targetSystem,
        int targetPosition,
        Instant departedAt,
        Instant arrivesAt
) {
}
