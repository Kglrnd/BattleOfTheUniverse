package de.kugidev.models;

import de.kugidev.models.galaxies.Galaxy;
import de.kugidev.models.galaxies.GalaxySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GalaxyTest {
    Galaxy galaxy;

    @BeforeEach
    void setUp() {
        galaxy = new Galaxy("Test");
    }

    @Test
    void addSystem() {
        galaxy.addSystem(new GalaxySystem("TestSystem"));
        assertNotNull(galaxy.getSysteme().getFirst());
    }

    @Test
    void isValid() {
        galaxy.addSystem(new GalaxySystem("TestSystem"));
        assertFalse(galaxy.isValid());
    }

    @Test
    void getName() {
        assertEquals("Test", galaxy.getName());
    }
}