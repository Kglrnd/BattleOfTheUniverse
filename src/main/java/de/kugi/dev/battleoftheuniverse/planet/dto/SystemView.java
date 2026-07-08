package de.kugi.dev.battleoftheuniverse.planet.dto;

import java.util.List;

public record SystemView(
        int galaxy,
        int system,
        List<SystemSlotView> slots
) {
}
