package de.kugi.dev.battleoftheuniverse.fleet;

import java.util.Map;

/**
 * Published when a colonize mission successfully founds a new colony; consumed by
 * {@code message} to notify the owner. {@code productionEfficiencies} maps each resource-producing
 * catalog building key to the efficiency freshly rolled for it on this planet (see
 * {@code building.BuildingService.initializeProducingBuildings}).
 */
public record ColonyFounded(Long ownerId, Long planetId, String planetName, double researchEfficiency,
                             Map<String, Double> productionEfficiencies) {
}
