package de.kugi.dev.battleoftheuniverse.planet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenamePlanetRequest(
        @NotBlank @Size(max = 50) String name
) {
}
