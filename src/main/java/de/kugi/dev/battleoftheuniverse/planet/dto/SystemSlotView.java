package de.kugi.dev.battleoftheuniverse.planet.dto;

public record SystemSlotView(
        int position,
        SlotStatus status,
        PlanetView planet
) {
    public static SystemSlotView occupied(int position, PlanetView planet) {
        return new SystemSlotView(position, SlotStatus.OCCUPIED, planet);
    }

    public static SystemSlotView destroyed(int position, PlanetView planet) {
        return new SystemSlotView(position, SlotStatus.DESTROYED, planet);
    }

    public static SystemSlotView free(int position) {
        return new SystemSlotView(position, SlotStatus.FREE, null);
    }

    public static SystemSlotView voidSlot(int position) {
        return new SystemSlotView(position, SlotStatus.VOID, null);
    }
}
