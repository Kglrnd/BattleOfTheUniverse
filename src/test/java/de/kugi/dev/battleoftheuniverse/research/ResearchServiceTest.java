package de.kugi.dev.battleoftheuniverse.research;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.catalog.TechnologyDefinition;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchStartResponse;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResearchServiceTest {

    @Mock
    private TechnologyRepository technologyRepository;
    @Mock
    private ResearchJobRepository jobRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private PlanetService planetService;
    @Mock
    private BuildingService buildingService;

    private ResearchService service;

    private static final Long USER_ID = 1L;
    private static final Long PLANET_ID = 42L;

    @BeforeEach
    void setUp() {
        service = new ResearchService(technologyRepository, jobRepository, catalogService, resourceService, planetService, buildingService);
    }

    private Planet activePlanet(double researchEfficiency) {
        Planet planet = new Planet("Lab Planet", USER_ID, 1, 1, 5, PlanetClass.TEMPERATE);
        planet.setId(PLANET_ID);
        planet.setResearchEfficiency(researchEfficiency);
        return planet;
    }

    @Test
    void startResearchRejectsWhenNoActiveResearchPlanetIsSet() {
        when(jobRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planetService.findActiveResearchPlanet(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startResearch(USER_ID, "energy_tech"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No active research planet");

        verify(resourceService, never()).debit(any(), any());
    }

    @Test
    void startResearchPaysFromAndTimesAgainstTheActiveResearchPlanet() {
        Planet planet = activePlanet(96.0);
        TechnologyDefinition definition = new TechnologyDefinition("energy_tech", "Energy Tech", "desc",
                new ResourceCost(100, 50, 0), 2.0, 100, DriveScope.NONE, 1.0, 0.0, List.of());

        when(jobRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planetService.findActiveResearchPlanet(USER_ID)).thenReturn(Optional.of(planet));
        when(catalogService.technology("energy_tech")).thenReturn(definition);
        when(technologyRepository.findByUserIdAndTechnologyKey(USER_ID, "energy_tech")).thenReturn(Optional.empty());
        when(catalogService.costFor(definition, 1)).thenReturn(new ResourceCost(100, 50, 0));
        when(catalogService.researchTimeFor(definition, 1)).thenReturn(Duration.ofSeconds(100));

        ResearchStartResponse response = service.startResearch(USER_ID, "energy_tech");

        assertThat(response.targetLevel()).isEqualTo(1);
        verify(resourceService).debit(PLANET_ID, new ResourceCost(100, 50, 0));

        ArgumentCaptor<ResearchJob> captor = ArgumentCaptor.forClass(ResearchJob.class);
        verify(jobRepository).save(captor.capture());
        // 96% suitability -> (2 - 0.96) = 1.04x the base duration.
        assertThat(Duration.between(captor.getValue().getStartedAt(), captor.getValue().getEndsAt()).toSeconds())
                .isEqualTo(104);
    }

    @Test
    void startResearchOnlyChecksTheBuildingRequirementOnTheActiveResearchPlanet() {
        Planet planet = activePlanet(100.0);
        Requirement labRequirement = new Requirement(RequirementType.BUILDING, "research_lab", 2);
        TechnologyDefinition definition = new TechnologyDefinition("advanced_tech", "Advanced Tech", "desc",
                new ResourceCost(100, 50, 0), 2.0, 100, DriveScope.NONE, 1.0, 0.0, List.of(labRequirement));

        when(jobRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planetService.findActiveResearchPlanet(USER_ID)).thenReturn(Optional.of(planet));
        when(catalogService.technology("advanced_tech")).thenReturn(definition);
        when(buildingService.levelOf(PLANET_ID, "research_lab")).thenReturn(1);
        when(catalogService.building("research_lab")).thenReturn(new BuildingDefinition(
                "research_lab", "Research Lab", "desc", new ResourceCost(200, 400, 200), 1.19, 180, ResourceKey.NONE, 0, 80, List.of()));

        assertThatThrownBy(() -> service.startResearch(USER_ID, "advanced_tech"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Requirements not met");

        verify(resourceService, never()).debit(any(), any());
        verify(planetService, never()).listMine(any());
    }

    @Test
    void activateResearchPlanetRejectsPlanetsWithoutAResearchLab() {
        when(buildingService.levelOf(PLANET_ID, "research_lab")).thenReturn(0);

        assertThatThrownBy(() -> service.activateResearchPlanet(USER_ID, PLANET_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Research Lab");

        verify(planetService, never()).activateResearchPlanet(any(), any());
    }

    private TechnologyDefinition driveDefinition(String key, DriveScope scope) {
        return new TechnologyDefinition(key, key, "desc",
                new ResourceCost(100, 50, 0), 1.1, 60, scope, 1.0, 0.1, List.of());
    }

    @Test
    void speedMultiplierForDriveRejectsADriveTooWideScopedForTheMission() {
        // A GALAXY-scoped drive is overkill for a same-system hop and shouldn't be offered for one.
        when(technologyRepository.findByUserIdAndTechnologyKey(USER_ID, "hyperspace_drive"))
                .thenReturn(Optional.of(new Technology(USER_ID, "hyperspace_drive", 5)));
        when(catalogService.technology("hyperspace_drive")).thenReturn(driveDefinition("hyperspace_drive", DriveScope.GALAXY));

        assertThat(service.speedMultiplierForDrive(USER_ID, "hyperspace_drive", DriveScope.SYSTEM)).isEmpty();
    }

    @Test
    void speedMultiplierForDriveAllowsANarrowerDriveOnAWiderMission() {
        // A SYSTEM-scoped drive can still make a cross-galaxy trip, just very slowly (see distance-based ETA).
        when(technologyRepository.findByUserIdAndTechnologyKey(USER_ID, "chemical_drive"))
                .thenReturn(Optional.of(new Technology(USER_ID, "chemical_drive", 5)));
        when(catalogService.technology("chemical_drive")).thenReturn(driveDefinition("chemical_drive", DriveScope.SYSTEM));

        assertThat(service.speedMultiplierForDrive(USER_ID, "chemical_drive", DriveScope.GALAXY)).isPresent();
    }

    @Test
    void listAvailableDrivesExcludesDrivesTooWideScopedForTheMission() {
        when(technologyRepository.findByUserId(USER_ID)).thenReturn(List.of(
                new Technology(USER_ID, "chemical_drive", 5),
                new Technology(USER_ID, "impulse_drive", 3),
                new Technology(USER_ID, "hyperspace_drive", 1)
        ));
        when(catalogService.technology("chemical_drive")).thenReturn(driveDefinition("chemical_drive", DriveScope.SYSTEM));
        when(catalogService.technology("impulse_drive")).thenReturn(driveDefinition("impulse_drive", DriveScope.INTER_SYSTEM));
        when(catalogService.technology("hyperspace_drive")).thenReturn(driveDefinition("hyperspace_drive", DriveScope.GALAXY));

        assertThat(service.listAvailableDrives(USER_ID, DriveScope.SYSTEM))
                .extracting(option -> option.key())
                .containsExactly("chemical_drive");
    }
}
