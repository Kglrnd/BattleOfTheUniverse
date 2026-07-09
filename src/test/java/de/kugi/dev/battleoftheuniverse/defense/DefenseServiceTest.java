package de.kugi.dev.battleoftheuniverse.defense;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DefenseDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.defense.dto.TowerBuildResponse;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenseServiceTest {

    private static final Long PLANET_ID = 1L;
    private static final DefenseDefinition LIGHT_TOWER = new DefenseDefinition(
            "light_defense_tower", "Light Defense Tower", "desc", 50, new ResourceCost(2000, 500, 0), 300,
            List.of(new Requirement(RequirementType.BUILDING, "defense_facility", 1)));

    @Mock
    private TowerRepository towerRepository;
    @Mock
    private DefenseJobRepository jobRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private BuildingService buildingService;

    private DefenseService service;

    @BeforeEach
    void setUp() {
        service = new DefenseService(towerRepository, jobRepository, catalogService, resourceService, buildingService);
    }

    @Test
    void queueTowerRejectsWhenTheRequiredBuildingLevelIsNotMet() {
        when(jobRepository.findByPlanetId(PLANET_ID)).thenReturn(Optional.empty());
        when(catalogService.defense("light_defense_tower")).thenReturn(LIGHT_TOWER);
        when(buildingService.levelOf(PLANET_ID, "defense_facility")).thenReturn(0);
        when(catalogService.building("defense_facility")).thenReturn(
                new de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition(
                        "defense_facility", "Defense Facility", "desc", new ResourceCost(300, 200, 0), 1.8, 300,
                        de.kugi.dev.battleoftheuniverse.catalog.ResourceKey.NONE, 0, List.of()));

        assertThatThrownBy(() -> service.queueTower(PLANET_ID, "light_defense_tower", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Requirements not met");
        verify(resourceService, never()).debit(any(), any());
    }

    @Test
    void queueTowerDebitsCostAndStartsAJobWhenRequirementsAreMet() {
        when(jobRepository.findByPlanetId(PLANET_ID)).thenReturn(Optional.empty());
        when(catalogService.defense("light_defense_tower")).thenReturn(LIGHT_TOWER);
        when(buildingService.levelOf(PLANET_ID, "defense_facility")).thenReturn(1);

        TowerBuildResponse response = service.queueTower(PLANET_ID, "light_defense_tower", 3);

        assertThat(response.towerKey()).isEqualTo("light_defense_tower");
        assertThat(response.quantity()).isEqualTo(3);
        verify(resourceService).debit(PLANET_ID, new ResourceCost(6000, 1500, 0));
        verify(jobRepository).save(any(DefenseJob.class));
    }

    @Test
    void queueTowerRejectsWhenAJobIsAlreadyInProgress() {
        when(jobRepository.findByPlanetId(PLANET_ID))
                .thenReturn(Optional.of(new DefenseJob(PLANET_ID, "light_defense_tower", 2, Instant.now(), Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.queueTower(PLANET_ID, "light_defense_tower", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already in progress");
        verify(resourceService, never()).debit(any(), any());
    }

    @Test
    void completeDueJobsCreditsTowersAndRemovesTheJob() {
        DefenseJob dueJob = new DefenseJob(PLANET_ID, "light_defense_tower", 4, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        when(jobRepository.findByEndsAtBefore(any(Instant.class))).thenReturn(List.of(dueJob));
        Tower existing = new Tower(PLANET_ID, "light_defense_tower", 2);
        when(towerRepository.findByPlanetIdAndTowerKey(PLANET_ID, "light_defense_tower")).thenReturn(Optional.of(existing));

        service.completeDueJobs();

        assertThat(existing.getQuantity()).isEqualTo(6);
        verify(towerRepository).save(existing);
        verify(jobRepository).delete(dueJob);
    }

    @Test
    void applyLossesReducesQuantityAndDeletesTheRowWhenFullyDestroyed() {
        Tower existing = new Tower(PLANET_ID, "light_defense_tower", 5);
        when(towerRepository.findByPlanetIdAndTowerKey(PLANET_ID, "light_defense_tower")).thenReturn(Optional.of(existing));

        service.applyLosses(PLANET_ID, java.util.Map.of("light_defense_tower", 5));

        verify(towerRepository).delete(existing);
        verify(towerRepository, never()).save(any());
    }

    @Test
    void applyLossesSavesTheReducedQuantityWhenSomeSurvive() {
        Tower existing = new Tower(PLANET_ID, "light_defense_tower", 5);
        when(towerRepository.findByPlanetIdAndTowerKey(PLANET_ID, "light_defense_tower")).thenReturn(Optional.of(existing));

        service.applyLosses(PLANET_ID, java.util.Map.of("light_defense_tower", 2));

        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(towerRepository).save(existing);
    }
}
