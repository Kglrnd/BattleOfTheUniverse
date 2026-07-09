package de.kugi.dev.battleoftheuniverse.defense.dto;

import jakarta.validation.constraints.Positive;

public record BuildTowerRequest(@Positive int quantity) {
}
