package de.kugi.dev.battleoftheuniverse.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Local-dev convenience: ensures an admin account exists so the JSON-Forms catalog
 * editor has someone to log in as without a manual SQL insert against the H2 file DB.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        User admin = new User(ADMIN_USERNAME, "admin@battleoftheuniverse.local", passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
