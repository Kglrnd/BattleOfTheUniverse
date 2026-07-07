package de.kugi.dev.battleoftheuniverse.user;

/**
 * Published after a new account is persisted. Other modules (e.g. {@code planet})
 * react to this via {@code @ApplicationModuleListener} instead of being called
 * directly, so {@code user} never needs to know who cares about new registrations.
 */
public record UserRegistered(Long userId, String username) {
}
