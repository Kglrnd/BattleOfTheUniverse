package de.kugi.dev.battleoftheuniverse.defense.dto;

/** A single unmet prerequisite, resolved to a human-readable label for display. */
public record LockedRequirement(String label, int requiredLevel, int currentLevel) {
}
