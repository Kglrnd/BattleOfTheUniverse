package de.kugi.dev.battleoftheuniverse.user;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game.dev")
public record DevAccountsProperties(boolean seedAccounts, @Nullable Account admin, @Nullable Account player) {

    public record Account(String username, String email, String password) {
    }
}
