package de.kugi.dev.battleoftheuniverse.building.dto;

/** One row in a production-per-level table - see {@link ResourceProductionView}. */
public record ProductionLevelView(int level, double productionPerHour, boolean currentLevel) {
}
