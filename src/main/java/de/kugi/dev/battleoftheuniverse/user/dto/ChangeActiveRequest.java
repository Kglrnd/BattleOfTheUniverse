package de.kugi.dev.battleoftheuniverse.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeActiveRequest(@NotNull Boolean active) {
}
