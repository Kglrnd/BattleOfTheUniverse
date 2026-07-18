package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Ticks resource production. Lives here (not in {@code resource}) because it needs
 * building levels, which {@code resource} deliberately knows nothing about — this
 * keeps the resource ledger a one-directional dependency of {@code building} instead
 * of a cycle between the two.
 */
@Component
@RequiredArgsConstructor
public class ProductionScheduler {

    private final PlanetBuildingRepository buildingRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;

    /**
     * Grouped by planet first, so a planet producing several resources is applied in one
     * transaction (see {@link ResourceService#applyProduction(Long, Map)}) instead of one
     * transaction per producing building game-wide.
     */
    @Scheduled(fixedRate = 10_000)
    public void tick() {
        Map<Long, Map<ResourceKey, Double>> hourlyRatesByPlanet = new HashMap<>();
        for (PlanetBuilding building : buildingRepository.findAll()) {
            BuildingDefinition definition = catalogService.building(building.getBuildingKey());
            if (definition.producesResource() == ResourceKey.NONE) {
                continue;
            }
            double hourlyRate = catalogService.productionPerHour(definition, building.getLevel())
                    * (building.getProductionEfficiency() / 100.0);
            hourlyRatesByPlanet.computeIfAbsent(building.getPlanetId(), id -> new HashMap<>())
                    .merge(definition.producesResource(), hourlyRate, Double::sum);
        }
        hourlyRatesByPlanet.forEach(resourceService::applyProduction);
    }
}
