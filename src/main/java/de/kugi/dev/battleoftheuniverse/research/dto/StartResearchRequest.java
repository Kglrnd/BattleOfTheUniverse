package de.kugi.dev.battleoftheuniverse.research.dto;

import jakarta.validation.constraints.NotNull;

public record StartResearchRequest(@NotNull Long planetId) {
}
