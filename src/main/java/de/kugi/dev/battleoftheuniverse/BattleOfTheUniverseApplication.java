package de.kugi.dev.battleoftheuniverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BattleOfTheUniverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleOfTheUniverseApplication.class, args);
    }
}
