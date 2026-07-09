package de.kugi.dev.battleoftheuniverse.defense.dto;

import java.time.Instant;

public record TowerBuildResponse(String towerKey, int quantity, Instant endsAt) {
}
