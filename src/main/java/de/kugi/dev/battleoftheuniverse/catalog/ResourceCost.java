package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * Cost expressed in the three buildable/storable resources. Energy is production-only
 * and is never spent on construction or research, so it has no place here.
 */
public record ResourceCost(long metal, long crystal, long deuterium) {

    public static final ResourceCost ZERO = new ResourceCost(0, 0, 0);

    public ResourceCost scaled(double factor) {
        return new ResourceCost(
                Math.round(metal * factor),
                Math.round(crystal * factor),
                Math.round(deuterium * factor)
        );
    }

    public boolean isAffordable(long availableMetal, long availableCrystal, long availableDeuterium) {
        return metal <= availableMetal && crystal <= availableCrystal && deuterium <= availableDeuterium;
    }
}
