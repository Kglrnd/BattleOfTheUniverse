package de.kugi.dev.battleoftheuniverse.planet;

import java.util.random.RandomGenerator;

/**
 * Shared "uniform in [85.00, 109.99], fixed for the entity's lifetime" efficiency roll used by
 * both a planet's research efficiency and (via {@code building}, which is allowed to depend on
 * this module) a resource-producing building's production efficiency.
 */
public final class EfficiencyRoll {

    private static final int MIN_HUNDREDTHS = 8500;
    private static final int MAX_HUNDREDTHS = 10999;

    private EfficiencyRoll() {
    }

    public static double roll(RandomGenerator random) {
        int hundredths = MIN_HUNDREDTHS + random.nextInt(MAX_HUNDREDTHS - MIN_HUNDREDTHS + 1);
        return hundredths / 100.0;
    }
}
