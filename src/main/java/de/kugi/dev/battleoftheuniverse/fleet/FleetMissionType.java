package de.kugi.dev.battleoftheuniverse.fleet;

public enum FleetMissionType {
    /** Founds a new colony at an empty, colonizable slot. Consumes the colony ships. */
    COLONIZE,
    /** Relocates ships to one of the sender's own planets. Ships are not consumed. */
    STATION,
    /**
     * Sent at another player's planet. On arrival, {@code combat} resolves a battle against
     * the target's defense towers and stationed fleet; survivors return to the origin planet.
     */
    ATTACK,
    /**
     * Sends espionage probes at another player's planet to gather intel on its stationed
     * fleet and resources. Success depends on the sender's espionage research level; on
     * failure the target is notified. Ships always return to the origin planet either way.
     */
    ESPIONAGE,
    /**
     * Sends resource cargo to one of the sender's own planets. Ships are not consumed -
     * they're stationed at the destination exactly like {@link #STATION} - and the cargo is
     * delivered on arrival.
     */
    TRANSPORT,
    /**
     * Sent at another player's colony (never a homeworld) to permanently destroy it. Requires
     * at least one Orbital Bomb and one Galaxy Class escort. If the target is undefended the
     * bomb has a ~95% chance to succeed; if defended, {@code combat} resolves a battle first
     * and the bomb only gets its chance if it survives. Survivors return to the origin planet.
     */
    BOMBARD,
    /**
     * Sent at another player's colony (never a homeworld) to capture it, transferring its
     * buildings/resources/stationed fleet intact to the attacker. Requires at least one
     * Invasion Unit and one Galaxy Class escort, and cannot be combined with {@link #BOMBARD}
     * ships in the same fleet. Same undefended-vs-defended success mechanics as
     * {@link #BOMBARD}. Survivors return to the origin planet.
     */
    INVADE
}
