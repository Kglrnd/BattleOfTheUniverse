package de.kugi.dev.battleoftheuniverse.fleet;

public enum FleetMissionType {
    /** Founds a new colony at an empty, colonizable slot. Consumes the colony ships. */
    COLONIZE,
    /** Relocates ships to one of the sender's own planets. Ships are not consumed. */
    STATION,
    /**
     * Sent at another player's planet. There's no combat resolution yet, so on arrival the
     * fleet simply turns around and its ships are credited back to the origin planet.
     */
    ATTACK
}
