package de.kugi.dev.battleoftheuniverse.planet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic per-system slot layout: which of the {@value #MAX_POSITIONS} possible orbital
 * positions in a system actually hold a colonizable slot. Every system has between
 * {@value #MIN_USABLE_SLOTS} and {@value #MAX_POSITIONS} usable slots, and which positions those
 * are varies per system (e.g. a system might be missing positions 3, 7 and 12 rather than always
 * trailing off at the end). Re-derived from (galaxy, system) on every call - seeded off those
 * coordinates - instead of being persisted, so it stays stable without needing its own table.
 */
final class SystemLayout {

    static final int GALAXY_COUNT = 5;
    static final int SYSTEMS_PER_GALAXY = 100;
    static final int MAX_POSITIONS = 15;
    static final int MIN_USABLE_SLOTS = 10;

    private SystemLayout() {
    }

    static boolean isInBounds(int galaxy, int system) {
        return galaxy >= 1 && galaxy <= GALAXY_COUNT && system >= 1 && system <= SYSTEMS_PER_GALAXY;
    }

    /** Positions 1..{@value #MAX_POSITIONS} that hold a usable slot in this system. */
    static Set<Integer> usablePositions(int galaxy, int system) {
        Random random = new Random(seed(galaxy, system));
        int slotCount = MIN_USABLE_SLOTS + random.nextInt(MAX_POSITIONS - MIN_USABLE_SLOTS + 1);

        List<Integer> positions = new ArrayList<>(MAX_POSITIONS);
        for (int position = 1; position <= MAX_POSITIONS; position++) {
            positions.add(position);
        }
        Collections.shuffle(positions, random);

        return new TreeSet<>(positions.subList(0, slotCount));
    }

    private static long seed(int galaxy, int system) {
        return (long) galaxy * 100_003L + system;
    }
}
