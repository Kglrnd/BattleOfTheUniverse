package de.kugi.dev.battleoftheuniverse.user;

public record UserView(Long id, String username, String email, Role role, String preferredLanguage) {
}
