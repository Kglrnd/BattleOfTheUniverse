package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMissionType;

import java.time.Instant;

public record FleetMovementView(
        Long id,
        Long originPlanetId,
        String shipKey,
        int quantity,
        FleetMissionType missionType,
        int targetGalaxy,
        int targetSystem,
        int targetPosition,
        Instant departedAt,
        Instant arrivesAt
) {
}
