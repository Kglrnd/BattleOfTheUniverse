package de.kugi.dev.battleoftheuniverse.user.dto;

import de.kugi.dev.battleoftheuniverse.user.Role;

import java.time.Instant;

public record AdminUserView(
        Long id,
        String username,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {
}
