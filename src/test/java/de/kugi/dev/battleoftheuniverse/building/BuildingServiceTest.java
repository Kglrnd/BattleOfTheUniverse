package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private PlanetBuildingRepository buildingRepository;
    @Mock
    private ConstructionJobRepository jobRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private PlanetService planetService;

    private BuildingService service;

    private static final Long PLANET_ID = 1L;
    private static final BuildingDefinition METAL_MINE = new BuildingDefinition(
            "metal_mine", "Metal Mine", "desc",
            new ResourceCost(60, 15, 0), 1.5, 60, ResourceKey.METAL, 30, 8, List.of());
    private static final BuildingDefinition RESEARCH_LAB = new BuildingDefinition(
            "research_lab", "Research Lab", "desc",
            new ResourceCost(200, 400, 200), 2.0, 300, ResourceKey.NONE, 0, 10, List.of());

    @BeforeEach
    void setUp() {
        service = new BuildingService(buildingRepository, jobRepository, catalogService, resourceService, planetService);
    }

    @Test
    void upgradeStartsAConstructionJobAtLevelOneWhenBuildingIsNew() {
        when(jobRepository.findByPlanetId(PLANET_ID)).thenReturn(Optional.empty());
        when(catalogService.building("metal_mine")).thenReturn(METAL_MINE);
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "metal_mine")).thenReturn(Optional.empty());
        when(catalogService.costFor(METAL_MINE, 1)).thenReturn(new ResourceCost(60, 15, 0));
        when(catalogService.buildTimeFor(METAL_MINE, 1)).thenReturn(Duration.ofSeconds(60));

        UpgradeResponse response = service.upgrade(PLANET_ID, "metal_mine");

        assertThat(response.targetLevel()).isEqualTo(1);
        verify(resourceService).debit(PLANET_ID, new ResourceCost(60, 15, 0));
        verify(jobRepository).save(any(ConstructionJob.class));
    }

    @Test
    void upgradeRejectsWhenAConstructionIsAlreadyInProgress() {
        when(jobRepository.findByPlanetId(PLANET_ID))
                .thenReturn(Optional.of(new ConstructionJob(PLANET_ID, "metal_mine", 2, Instant.now(), Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.upgrade(PLANET_ID, "crystal_mine"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already in progress");

        verify(resourceService, never()).debit(any(), any());
    }

    @Test
    void completeDueJobsBumpsLevelAndRemovesTheJob() {
        ConstructionJob dueJob = new ConstructionJob(PLANET_ID, "metal_mine", 2, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(jobRepository.findByEndsAtBefore(any(Instant.class))).thenReturn(java.util.List.of(dueJob));
        PlanetBuilding existing = new PlanetBuilding(PLANET_ID, "metal_mine", 1);
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "metal_mine")).thenReturn(Optional.of(existing));

        service.completeDueJobs();

        assertThat(existing.getLevel()).isEqualTo(2);
        verify(buildingRepository).save(existing);
        verify(jobRepository).delete(dueJob);
    }

    @Test
    void completeDueJobsRollsAProductionEfficiencyForANewlyBuiltProducingBuilding() {
        ConstructionJob dueJob = new ConstructionJob(PLANET_ID, "metal_mine", 1, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(jobRepository.findByEndsAtBefore(any(Instant.class))).thenReturn(List.of(dueJob));
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "metal_mine")).thenReturn(Optional.empty());
        when(catalogService.building("metal_mine")).thenReturn(METAL_MINE);

        service.completeDueJobs();

        var captor = org.mockito.ArgumentCaptor.forClass(PlanetBuilding.class);
        verify(buildingRepository).save(captor.capture());
        assertThat(captor.getValue().getProductionEfficiency()).isBetween(85.0, 109.99);
    }

    @Test
    void completeDueJobsLeavesProductionEfficiencyAtDefaultForANewlyBuiltNonProducingBuilding() {
        ConstructionJob dueJob = new ConstructionJob(PLANET_ID, "research_lab", 1, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(jobRepository.findByEndsAtBefore(any(Instant.class))).thenReturn(List.of(dueJob));
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "research_lab")).thenReturn(Optional.empty());
        when(catalogService.building("research_lab")).thenReturn(RESEARCH_LAB);

        service.completeDueJobs();

        var captor = org.mockito.ArgumentCaptor.forClass(PlanetBuilding.class);
        verify(buildingRepository).save(captor.capture());
        assertThat(captor.getValue().getProductionEfficiency()).isEqualTo(100.0);
    }

    @Test
    void listForPlanetExposesProductionEfficiencyOnlyForBuiltProducingBuildingsAndResearchEfficiencyOnlyForTheResearchLab() {
        Planet planet = new Planet("Home", 9L, 1, 1, 1, PlanetClass.TEMPERATE);
        planet.setResearchEfficiency(97.5);
        when(planetService.getById(PLANET_ID)).thenReturn(planet);
        when(jobRepository.findByPlanetId(PLANET_ID)).thenReturn(Optional.empty());
        when(catalogService.buildings()).thenReturn(List.of(METAL_MINE, RESEARCH_LAB));

        PlanetBuilding metalMineRow = new PlanetBuilding(PLANET_ID, "metal_mine", 3);
        metalMineRow.setProductionEfficiency(92.1);
        when(buildingRepository.findByPlanetId(PLANET_ID)).thenReturn(List.of(metalMineRow));
        when(catalogService.costFor(any(BuildingDefinition.class), any(Integer.class))).thenReturn(new ResourceCost(1, 1, 1));
        when(catalogService.buildTimeFor(any(BuildingDefinition.class), any(Integer.class))).thenReturn(Duration.ofSeconds(1));

        List<BuildingView> views = service.listForPlanet(PLANET_ID);

        BuildingView metalMineView = views.stream().filter(v -> v.key().equals("metal_mine")).findFirst().orElseThrow();
        assertThat(metalMineView.productionEfficiency()).isEqualTo(92.1);
        assertThat(metalMineView.researchEfficiency()).isNull();

        BuildingView researchLabView = views.stream().filter(v -> v.key().equals("research_lab")).findFirst().orElseThrow();
        assertThat(researchLabView.researchEfficiency()).isEqualTo(97.5);
        assertThat(researchLabView.productionEfficiency()).isNull();
    }

    @Test
    void initializeStarterCreatesTheMainBuildingAndRollsEfficiencyForEveryProducingBuilding() {
        when(catalogService.buildings()).thenReturn(List.of(METAL_MINE, RESEARCH_LAB));
        when(catalogService.building("metal_mine")).thenReturn(METAL_MINE);
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "metal_mine")).thenReturn(Optional.empty());
        when(buildingRepository.save(any(PlanetBuilding.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initializeStarter(PLANET_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(PlanetBuilding.class);
        // main_building (starter) + metal_mine (producing) - research_lab isn't a producer, so it gets no row here.
        verify(buildingRepository, times(2)).save(captor.capture());
        PlanetBuilding mainBuilding = captor.getAllValues().stream()
                .filter(b -> b.getBuildingKey().equals("main_building")).findFirst().orElseThrow();
        assertThat(mainBuilding.getLevel()).isEqualTo(1);
        PlanetBuilding metalMine = captor.getAllValues().stream()
                .filter(b -> b.getBuildingKey().equals("metal_mine")).findFirst().orElseThrow();
        assertThat(metalMine.getLevel()).isEqualTo(0);
        assertThat(metalMine.getProductionEfficiency()).isBetween(85.0, 109.99);
    }

    @Test
    void initializeProducingBuildingsIsIdempotentAndReusesAnExistingRowsStoredEfficiency() {
        when(catalogService.buildings()).thenReturn(List.of(METAL_MINE));
        PlanetBuilding existing = new PlanetBuilding(PLANET_ID, "metal_mine", 0);
        existing.setProductionEfficiency(91.5);
        when(buildingRepository.findByPlanetIdAndBuildingKey(PLANET_ID, "metal_mine")).thenReturn(Optional.of(existing));

        Map<String, Double> result = service.initializeProducingBuildings(PLANET_ID);

        assertThat(result).containsEntry("metal_mine", 91.5);
        verify(buildingRepository, never()).save(any());
    }
}
