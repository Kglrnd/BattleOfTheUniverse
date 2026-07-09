package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(fixedRate = 10_000)
    public void tick() {
        for (PlanetBuilding building : buildingRepository.findByLevelGreaterThan(0)) {
            BuildingDefinition definition = catalogService.building(building.getBuildingKey());
            if (definition.producesResource() == ResourceKey.NONE) {
                continue;
            }
            double hourlyRate = catalogService.productionPerHour(definition, building.getLevel());
            resourceService.applyProduction(building.getPlanetId(), definition.producesResource(), hourlyRate);
        }
    }
}
