package de.kugi.dev.battleoftheuniverse.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;

/**
 * Shared "which requirements aren't met yet" computation, previously duplicated (with subtly
 * different level-lookup logic) in {@code building}, {@code research}, {@code fleet} and
 * {@code defense}. Callers supply a {@code currentLevel} lookup that resolves a requirement's
 * current level however their own scoping works (planet-local building level, account-wide
 * technology level, a specific "active" planet, etc); a module that doesn't gate on a given
 * {@link RequirementType} at all can simply return {@code Integer.MAX_VALUE} for it, so that
 * type of requirement can never appear as unmet - preserving the "skip" behavior those modules
 * had before this was extracted.
 */
public final class RequirementChecker {

    /** An unmet prerequisite: the caller maps this into its own module's {@code LockedRequirement} DTO. */
    public record Gap(String label, int requiredLevel, int currentLevel) {
    }

    private RequirementChecker() {
    }

    public static List<Gap> unmet(CatalogService catalogService, List<Requirement> requirements,
                                   ToIntBiFunction<RequirementType, String> currentLevel) {
        List<Gap> missing = new ArrayList<>();
        for (Requirement requirement : requirements) {
            int level = currentLevel.applyAsInt(requirement.type(), requirement.key());
            if (level < requirement.level()) {
                String label = requirement.type() == RequirementType.TECHNOLOGY
                        ? catalogService.technology(requirement.key()).name()
                        : catalogService.building(requirement.key()).name();
                missing.add(new Gap(label, requirement.level(), level));
            }
        }
        return missing;
    }
}
