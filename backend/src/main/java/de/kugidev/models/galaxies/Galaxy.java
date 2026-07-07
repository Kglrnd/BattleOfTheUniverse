package de.kugidev.models;

import java.util.ArrayList;
import java.util.List;

public class Galaxy {
    private final String name;
    private final List<GalaxySystem> system;

    public Galaxy(String name) {
        this.name = name;
        this.system = new ArrayList<>();

    }

        public void addSystem(GalaxySystem system) {
            // Minimalwert-Check auf "mindestens 150 Systeme"
            if (this.system.size() >= 250) {
                throw new IllegalArgumentException("Eine Galaxie kann maximal 250 Systeme enthalten.");
            }
            this.system.add(system);
        }

        public boolean isValid() {
            return system.size() >= 150; // Galaxie muss mindestens 150 Systeme haben
        }

        public List<GalaxySystem> getSysteme() {
            return system;
        }

        public String getName() {
            return name;
        }
}
