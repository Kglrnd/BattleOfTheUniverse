package de.kugi.dev.battleoftheuniverse.planet;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemLayoutTest {

    @Test
    void everySystemHasBetweenTenAndFifteenUsableSlots() {
        for (int galaxy = 1; galaxy <= SystemLayout.GALAXY_COUNT; galaxy++) {
            for (int system = 1; system <= SystemLayout.SYSTEMS_PER_GALAXY; system++) {
                Set<Integer> usable = SystemLayout.usablePositions(galaxy, system);
                assertThat(usable.size()).isBetween(SystemLayout.MIN_USABLE_SLOTS, SystemLayout.MAX_POSITIONS);
                assertThat(usable).allSatisfy(position ->
                        assertThat(position).isBetween(1, SystemLayout.MAX_POSITIONS));
            }
        }
    }

    @Test
    void layoutIsDeterministicForTheSameCoordinates() {
        assertThat(SystemLayout.usablePositions(2, 37)).isEqualTo(SystemLayout.usablePositions(2, 37));
    }

    @Test
    void slotCountsAndPositionsVaryAcrossSystems() {
        Set<Integer> slotCounts = new HashSet<>();
        Set<Set<Integer>> distinctLayouts = new HashSet<>();
        for (int system = 1; system <= SystemLayout.SYSTEMS_PER_GALAXY; system++) {
            Set<Integer> usable = SystemLayout.usablePositions(1, system);
            slotCounts.add(usable.size());
            distinctLayouts.add(usable);
        }

        assertThat(slotCounts).as("slot count should vary between systems, not be a fixed 15").hasSizeGreaterThan(1);
        assertThat(distinctLayouts).as("which positions are usable should differ, not just the count")
                .hasSizeGreaterThan(1);
    }

    @Test
    void boundsCheckRejectsOutOfRangeCoordinates() {
        assertThat(SystemLayout.isInBounds(1, 1)).isTrue();
        assertThat(SystemLayout.isInBounds(SystemLayout.GALAXY_COUNT, SystemLayout.SYSTEMS_PER_GALAXY)).isTrue();
        assertThat(SystemLayout.isInBounds(0, 1)).isFalse();
        assertThat(SystemLayout.isInBounds(1, 0)).isFalse();
        assertThat(SystemLayout.isInBounds(SystemLayout.GALAXY_COUNT + 1, 1)).isFalse();
        assertThat(SystemLayout.isInBounds(1, SystemLayout.SYSTEMS_PER_GALAXY + 1)).isFalse();
    }
}
