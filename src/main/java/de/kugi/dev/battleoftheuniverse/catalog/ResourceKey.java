package de.kugi.dev.battleoftheuniverse.catalog;

public enum ResourceKey {
    METAL("Metal"),
    CRYSTAL("Crystal"),
    DEUTERIUM("Deuterium"),
    HYDROGEN("Hydrogen"),
    ENERGY("Energy"),
    /** Sentinel for "this building produces nothing" — kept out of JSON null/optional-schema territory. */
    NONE("None");

    private final String displayName;

    ResourceKey(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
