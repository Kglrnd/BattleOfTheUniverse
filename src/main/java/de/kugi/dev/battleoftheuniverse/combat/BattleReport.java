package de.kugi.dev.battleoftheuniverse.combat;

import java.time.Instant;

/**
 * Placeholder marking the {@code combat} module's intended boundary: resolving fleet
 * vs. fleet/defense engagements between two planets. Not wired up to anything yet —
 * once {@code fleet} grows movement/missions, an attack mission arriving at a hostile
 * planet is what will produce one of these.
 */
public record BattleReport(Long attackerPlanetId, Long defenderPlanetId, Instant occurredAt) {
}
