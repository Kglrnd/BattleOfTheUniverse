package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildingServiceTest {

    @Mock
    private PlanetBuildingRepository buildingRepository;
    @Mock
    private ConstructionJobRepository jobRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;

    private BuildingService service;

    private static final Long PLANET_ID = 1L;
    private static final BuildingDefinition METAL_MINE = new BuildingDefinition(
            "metal_mine", "Metal Mine", "desc",
            new ResourceCost(60, 15, 0), 1.5, 60, ResourceKey.METAL, 30);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BuildingService(buildingRepository, jobRepository, catalogService, resourceService);
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
}
