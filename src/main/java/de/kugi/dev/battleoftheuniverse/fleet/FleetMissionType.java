package de.kugi.dev.battleoftheuniverse.fleet;

/**
 * ATTACK is a natural follow-up once combat exists; not offered yet.
 */
public enum FleetMissionType {
    /** Founds a new colony at an empty, colonizable slot. Consumes the colony ships. */
    COLONIZE,
    /** Relocates ships to one of the sender's own planets. Ships are not consumed. */
    STATION
}
