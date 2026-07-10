package de.kugi.dev.battleoftheuniverse.highscore.dto;

public record HighscoreEntryDto(int rank, Long userId, String username, long score) {
}
