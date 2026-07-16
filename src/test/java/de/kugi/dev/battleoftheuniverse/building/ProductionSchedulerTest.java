package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionSchedulerTest {

    @Mock
    private PlanetBuildingRepository buildingRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;

    private ProductionScheduler scheduler;

    private static final Long PLANET_ID = 1L;
    private static final BuildingDefinition METAL_MINE = new BuildingDefinition(
            "metal_mine", "Metal Mine", "desc",
            new ResourceCost(60, 15, 0), 1.5, 60, ResourceKey.METAL, 30, 8, List.of());

    @BeforeEach
    void setUp() {
        scheduler = new ProductionScheduler(buildingRepository, catalogService, resourceService);
    }

    @Test
    void tickScalesHourlyRateByTheBuildingsProductionEfficiency() {
        PlanetBuilding building = new PlanetBuilding(PLANET_ID, "metal_mine", 5);
        building.setProductionEfficiency(80.0);
        when(buildingRepository.findByLevelGreaterThan(0)).thenReturn(List.of(building));
        when(catalogService.building("metal_mine")).thenReturn(METAL_MINE);
        when(catalogService.productionPerHour(METAL_MINE, 5)).thenReturn(150.0);

        scheduler.tick();

        // 150 base/hour * 80% efficiency = 120/hour
        verify(resourceService).applyProduction(eq(PLANET_ID), eq(ResourceKey.METAL), eq(120.0));
    }

    @Test
    void tickSkipsBuildingsThatDoNotProduceAResource() {
        PlanetBuilding building = new PlanetBuilding(PLANET_ID, "main_building", 3);
        when(buildingRepository.findByLevelGreaterThan(0)).thenReturn(List.of(building));
        BuildingDefinition mainBuilding = new BuildingDefinition(
                "main_building", "Main Building", "desc",
                new ResourceCost(0, 0, 0), 1.5, 60, ResourceKey.NONE, 0, 0, List.of());
        when(catalogService.building("main_building")).thenReturn(mainBuilding);

        scheduler.tick();

        verify(resourceService, org.mockito.Mockito.never()).applyProduction(eq(PLANET_ID), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble());
    }
}
