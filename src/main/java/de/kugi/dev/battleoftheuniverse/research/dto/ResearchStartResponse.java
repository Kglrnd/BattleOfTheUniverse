package de.kugi.dev.battleoftheuniverse.research.dto;

import java.time.Instant;

public record ResearchStartResponse(String technologyKey, int targetLevel, Instant endsAt) {
}
