package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * How far a researched drive lets a fleet travel. Ordinal order matters: a drive
 * covers its own scope and every narrower one (a GALAXY-capable drive can obviously
 * also cross a single system), so callers compare via {@code ordinal()}.
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
