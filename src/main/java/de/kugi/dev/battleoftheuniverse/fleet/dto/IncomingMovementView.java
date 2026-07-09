package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMissionType;

import java.time.Instant;
import java.util.List;

/** A fleet movement targeting one of the requesting player's own planets, sent by anyone. */
public record IncomingMovementView(
        Long id,
        List<ShipQuantity> ships,
        FleetMissionType missionType,
        Long originPlanetId,
        String originOwnerUsername,
        Long targetPlanetId,
        String targetPlanetName,
        Instant departedAt,
        Instant arrivesAt
) {
}
