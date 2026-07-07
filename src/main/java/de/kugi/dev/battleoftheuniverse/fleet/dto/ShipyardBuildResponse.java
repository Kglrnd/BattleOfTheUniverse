package de.kugi.dev.battleoftheuniverse.fleet.dto;

import java.time.Instant;

public record ShipyardBuildResponse(String shipKey, int quantity, Instant endsAt) {
}
