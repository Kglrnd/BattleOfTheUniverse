package de.kugi.dev.battleoftheuniverse.research.dto;

public record ResearchPlanetOption(
        Long planetId,
        String name,
        String coordinates,
        double researchEfficiency,
        int researchLabLevel,
        boolean active
) {
}
