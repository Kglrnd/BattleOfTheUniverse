package de.kugi.dev.battleoftheuniverse.user.dto;

import de.kugi.dev.battleoftheuniverse.user.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
