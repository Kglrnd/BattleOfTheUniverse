package de.kugi.dev.battleoftheuniverse.building.dto;

import java.time.Instant;

public record UpgradeResponse(String buildingKey, int targetLevel, Instant endsAt) {
}
