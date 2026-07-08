package de.kugi.dev.battleoftheuniverse.user.dto;

import de.kugi.dev.battleoftheuniverse.user.Role;
import de.kugi.dev.battleoftheuniverse.user.User;

import java.time.Instant;

public record AdminUserView(
        Long id,
        String username,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {
    public static AdminUserView from(User user) {
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
