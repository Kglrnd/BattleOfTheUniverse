package de.kugi.dev.battleoftheuniverse.planet.dto;

import de.kugi.dev.battleoftheuniverse.planet.Planet;

public record SystemSlotView(
        int position,
        SlotStatus status,
        PlanetView planet
) {
    public static SystemSlotView occupied(int position, Planet planet) {
        return new SystemSlotView(position, SlotStatus.OCCUPIED, PlanetView.from(planet));
    }

    public static SystemSlotView free(int position) {
        return new SystemSlotView(position, SlotStatus.FREE, null);
    }

    public static SystemSlotView voidSlot(int position) {
        return new SystemSlotView(position, SlotStatus.VOID, null);
    }
}
