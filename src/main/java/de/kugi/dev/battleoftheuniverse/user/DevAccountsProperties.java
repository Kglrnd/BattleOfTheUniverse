package de.kugi.dev.battleoftheuniverse.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game.dev")
public record DevAccountsProperties(boolean seedAccounts, Account admin, Account player) {

    public record Account(String username, String email, String password) {
    }
}
