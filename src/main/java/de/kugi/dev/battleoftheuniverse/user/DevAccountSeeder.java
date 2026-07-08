package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.RegisterRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Local-dev convenience: seeds one admin and one player account with distinct,
 * well-known credentials (see application-dev.yml) so the frontend can be exercised
 * as both roles without a manual SQL insert against the H2 file DB. Only active on
 * the "dev" profile, which is the default unless overridden (see application.yml).
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "game.dev", name = "seed-accounts", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DevAccountsProperties.class)
@Order(1)
public class DevAccountSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DevAccountsProperties properties;

    public DevAccountSeeder(UserRepository userRepository, UserService userService,
                             PasswordEncoder passwordEncoder, DevAccountsProperties properties) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin(properties.admin());
        seedPlayer(properties.player());
    }

    private void seedAdmin(DevAccountsProperties.Account account) {
        if (account == null || userRepository.existsByUsername(account.username())) {
            return;
        }
        User admin = new User(account.username(), account.email(), passwordEncoder.encode(account.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private void seedPlayer(DevAccountsProperties.Account account) {
        if (account == null || userRepository.existsByUsername(account.username())) {
            return;
        }
        // Goes through UserService so the usual UserRegistered event fires and the
        // player module reacts to it (see planet.UserRegistrationListener), keeping
        // this dev-only seeder from having to cross module boundaries itself.
        userService.register(new RegisterRequest(account.username(), account.email(), account.password()));
    }
}
