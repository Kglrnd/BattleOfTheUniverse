package de.kugi.dev.battleoftheuniverse.highscore.dto;

import java.util.List;

/**
 * {@code me} is the requesting user's own entry regardless of whether they place inside
 * {@code top} - the frontend decides what to show based on whether {@code me.rank() <= top.size()}.
 */
public record HighscoreResponseDto(List<HighscoreEntryDto> top, HighscoreEntryDto me) {
}
