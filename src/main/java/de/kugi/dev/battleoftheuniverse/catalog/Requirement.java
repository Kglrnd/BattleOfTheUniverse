package de.kugi.dev.battleoftheuniverse.catalog;

/**
 * A single prerequisite a building or technology definition can list: the referenced
 * building/technology (by catalog key) must be at least at {@code level} before the
 * gated definition becomes buildable/researchable.
 */
public record Requirement(RequirementType type, String key, int level) {
}
