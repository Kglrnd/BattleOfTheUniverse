package de.kugidev.battleoftheuniverse.config;

import de.kugidev.battleoftheuniverse.model.User;
import de.kugidev.battleoftheuniverse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Admin-Benutzer erstellen falls nicht vorhanden
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", passwordEncoder.encode("admin123"), User.Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Admin-Benutzer erstellt: admin / admin123");
        }

        // Test-Spieler erstellen falls nicht vorhanden
        if (!userRepository.existsByUsername("spieler1")) {
            User player = new User("spieler1", passwordEncoder.encode("spieler123"), User.Role.PLAYER);
            userRepository.save(player);
            System.out.println("✅ Test-Spieler erstellt: spieler1 / spieler123");
        }

        if (!userRepository.existsByUsername("kommandant")) {
            User player2 = new User("kommandant", passwordEncoder.encode("galaxy123"), User.Role.PLAYER);
            userRepository.save(player2);
            System.out.println("✅ Test-Spieler erstellt: kommandant / galaxy123");
        }
    }
}
