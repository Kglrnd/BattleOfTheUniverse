package de.kugidev.models;

import java.util.ArrayList;
import java.util.List;

public class GalaxySystem {

    private String name;
    private List<Planet> planeten;
    private List<Sun> sonnen;
    private List<SolarPanel> sonnenkollektoren;

    public GalaxySystem(String name) {
        this.name = name;
        this.planeten = new ArrayList<>();
        this.sonnen = new ArrayList<>();
        this.sonnenkollektoren = new ArrayList<>();
    }

    public void addPlanet(Planet planet) {
        if (planeten.size() >= 15) {
            throw new IllegalArgumentException("Ein System kann maximal 15 Planeten enthalten.");
        }
        planeten.add(planet);
    }

    public void addSonne(Sun sonne) {
        if (sonnen.size() >= 3) {
            throw new IllegalArgumentException("Ein System kann maximal 3 Sonnen haben.");
        }
        sonnen.add(sonne);
    }

    public void addSonnenkollektor(SolarPanel kollektor) {
        if (sonnenkollektoren.size() >= 10) {
            throw new IllegalArgumentException("Ein System kann maximal 10 Sonnenkollektoren haben.");
        }
        sonnenkollektoren.add(kollektor);
    }

    public boolean isValid() {
        // Minimalwerte 4 Planeten, 1 Sonne, 1 Sonnenkollektor prüfen
        return planeten.size() >= 4 && sonnen.size() >= 1 && sonnenkollektoren.size() >= 1;
    }

    public List<Planet> getPlaneten() {
        return planeten;
    }

    public List<Sun> getSonnen() {
        return sonnen;
    }

    public List<SolarPanel> getSonnenkollektoren() {
        return sonnenkollektoren;
    }

}
