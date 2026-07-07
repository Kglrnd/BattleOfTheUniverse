package de.kugi.dev.battleoftheuniverse;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(BattleOfTheUniverseApplication.class);

    @Test
    void modulesAreConsistent() {
        modules.verify();
    }

    @Test
    void writeDocumentationSnapshot() {
        modules.forEach(System.out::println);
    }
}
