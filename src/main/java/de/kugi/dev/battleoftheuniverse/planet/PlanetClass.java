package de.kugi.dev.battleoftheuniverse.planet;

public enum PlanetClass {
    TEMPERATE("Temperate", 1.0, true),
    OCEANIC("Oceanic", 0.9, true),
    DESERT("Desert", 0.8, true),
    ICE("Ice", 0.7, true),
    VOLCANIC("Volcanic", 0.6, true),
    GAS_GIANT("Gas Giant", 0.0, false);

    private final String displayName;
    private final double productionMultiplier;
    private final boolean habitable;

    PlanetClass(String displayName, double productionMultiplier, boolean habitable) {
        this.displayName = displayName;
        this.productionMultiplier = productionMultiplier;
        this.habitable = habitable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getProductionMultiplier() {
        return productionMultiplier;
    }

    public boolean isHabitable() {
        return habitable;
    }
}
