package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * How far a drive is designed to reach. A mission only offers drives whose scope is
 * at or below what the trip requires - a GALAXY drive is overkill for a same-system
 * hop and isn't offered for one. The reverse works, just slowly: a narrower drive
 * (e.g. SYSTEM) can still be picked for a wider trip, it simply takes a very long
 * time since travel time is driven by raw distance, not by scope. Ordinal order
 * matters, so callers compare via {@code ordinal()}.
 */
public enum DriveScope {
    /** Not a drive — the default for every non-propulsion technology. */
    NONE,
    /** Between positions within the same system. */
    SYSTEM,
    /** Between systems within the same galaxy. */
    INTER_SYSTEM,
    /** Between galaxies. */
    GALAXY
}
