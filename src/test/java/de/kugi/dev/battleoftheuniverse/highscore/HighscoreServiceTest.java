package de.kugi.dev.battleoftheuniverse.highscore;

import de.kugi.dev.battleoftheuniverse.building.PlanetBuilding;
import de.kugi.dev.battleoftheuniverse.building.PlanetBuildingRepository;
import de.kugi.dev.battleoftheuniverse.catalog.BuildingDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DefenseDefinition;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.defense.Tower;
import de.kugi.dev.battleoftheuniverse.defense.TowerRepository;
import de.kugi.dev.battleoftheuniverse.fleet.Ship;
import de.kugi.dev.battleoftheuniverse.fleet.ShipRepository;
import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreResponseDto;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetRepository;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighscoreServiceTest {

    private static final BuildingDefinition METAL_MINE = new BuildingDefinition(
            "metal_mine", "Metal Mine", "desc",
            new ResourceCost(60, 15, 0), 1.5, 60, ResourceKey.METAL, 30, 8, List.of());

    private static final ShipDefinition CRUISER = new ShipDefinition(
            "cruiser", "Cruiser", "desc", 400, 200, 15000, 50, 4,
            new ResourceCost(20000, 7000, 2000), 1800, 29);

    private static final ShipDefinition COLONY_SHIP = new ShipDefinition(
            "colony_ship", "Colony Ship", "desc", 0, 100, 2500, 1000, 6,
            new ResourceCost(10000, 20000, 10000), 3600, 0);

    private static final ShipDefinition TRANSPORT = new ShipDefinition(
            "small_cargo", "Transport Ship", "desc", 5, 10, 10000, 25000, 3,
            new ResourceCost(2000, 2000, 0), 600, 0);

    private static final DefenseDefinition LIGHT_TOWER = new DefenseDefinition(
            "light_defense_tower", "Light Defense Tower", "desc", 50,
            new ResourceCost(2000, 500, 0), 300, 3, List.of());

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlanetRepository planetRepository;
    @Mock
    private PlanetBuildingRepository planetBuildingRepository;
    @Mock
    private ShipRepository shipRepository;
    @Mock
    private TowerRepository towerRepository;
    @Mock
    private CatalogService catalogService;

    private HighscoreService service;

    @BeforeEach
    void setUp() {
        service = new HighscoreService(userRepository, planetRepository, planetBuildingRepository,
                shipRepository, towerRepository, catalogService);

        lenient().when(catalogService.building("metal_mine")).thenReturn(METAL_MINE);
        lenient().when(catalogService.ship("cruiser")).thenReturn(CRUISER);
        lenient().when(catalogService.ship("colony_ship")).thenReturn(COLONY_SHIP);
        lenient().when(catalogService.ship("small_cargo")).thenReturn(TRANSPORT);
        lenient().when(catalogService.defense("light_defense_tower")).thenReturn(LIGHT_TOWER);

        lenient().when(planetBuildingRepository.findByPlanetId(anyLong())).thenReturn(List.of());
        lenient().when(shipRepository.findByPlanetId(anyLong())).thenReturn(List.of());
        lenient().when(towerRepository.findByPlanetId(anyLong())).thenReturn(List.of());
    }

    private User user(long id, String username) {
        User user = new User(username, username + "@example.com", "hash");
        user.setId(id);
        return user;
    }

    private Planet planet(long id, long ownerId) {
        Planet planet = new Planet("Home", ownerId, 1, 1, (int) id, PlanetClass.TEMPERATE);
        planet.setId(id);
        return planet;
    }

    @Test
    void scoresBuildingsShipsAndTowersByCatalogPoints() {
        User alice = user(1L, "alice");
        when(userRepository.findAll()).thenReturn(List.of(alice));
        when(planetRepository.findByOwnerId(1L)).thenReturn(List.of(planet(10L, 1L)));
        when(planetBuildingRepository.findByPlanetId(10L))
                .thenReturn(List.of(new PlanetBuilding(10L, "metal_mine", 5)));
        when(shipRepository.findByPlanetId(10L))
                .thenReturn(List.of(new Ship(10L, "cruiser", 2)));
        when(towerRepository.findByPlanetId(10L))
                .thenReturn(List.of(new Tower(10L, "light_defense_tower", 4)));

        HighscoreResponseDto result = service.get(1L);

        // metal_mine: 5 levels * 8 points = 40; cruiser: 2 * 29 = 58; tower: 4 * 3 = 12 -> 110
        assertThat(result.me().score()).isEqualTo(110L);
        assertThat(result.me().rank()).isEqualTo(1);
        assertThat(result.me().userId()).isEqualTo(1L);
        assertThat(result.top()).hasSize(1);
        assertThat(result.top().getFirst().username()).isEqualTo("alice");
        assertThat(result.top().getFirst().userId()).isEqualTo(1L);
    }

    @Test
    void colonyShipsAndTransportsScoreZeroPoints() {
        User alice = user(1L, "alice");
        when(userRepository.findAll()).thenReturn(List.of(alice));
        when(planetRepository.findByOwnerId(1L)).thenReturn(List.of(planet(10L, 1L)));
        when(shipRepository.findByPlanetId(10L)).thenReturn(List.of(
                new Ship(10L, "colony_ship", 1),
                new Ship(10L, "small_cargo", 3)));

        HighscoreResponseDto result = service.get(1L);

        assertThat(result.me().score()).isZero();
    }

    @Test
    void usersWithoutAnyPlanetAreExcludedFromRanking() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));
        when(planetRepository.findByOwnerId(1L)).thenReturn(List.of(planet(10L, 1L)));
        when(planetRepository.findByOwnerId(2L)).thenReturn(List.of());
        when(planetBuildingRepository.findByPlanetId(10L))
                .thenReturn(List.of(new PlanetBuilding(10L, "metal_mine", 1)));

        HighscoreResponseDto result = service.get(1L);

        assertThat(result.top()).hasSize(1);
        assertThat(result.top().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void tiedScoresShareTheSameRank() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        User carol = user(3L, "carol");
        when(userRepository.findAll()).thenReturn(List.of(alice, bob, carol));
        when(planetRepository.findByOwnerId(1L)).thenReturn(List.of(planet(10L, 1L)));
        when(planetRepository.findByOwnerId(2L)).thenReturn(List.of(planet(20L, 2L)));
        when(planetRepository.findByOwnerId(3L)).thenReturn(List.of(planet(30L, 3L)));
        when(planetBuildingRepository.findByPlanetId(10L))
                .thenReturn(List.of(new PlanetBuilding(10L, "metal_mine", 5)));
        when(planetBuildingRepository.findByPlanetId(20L))
                .thenReturn(List.of(new PlanetBuilding(20L, "metal_mine", 5)));
        when(planetBuildingRepository.findByPlanetId(30L))
                .thenReturn(List.of(new PlanetBuilding(30L, "metal_mine", 1)));

        HighscoreResponseDto aliceResult = service.get(1L);
        HighscoreResponseDto carolResult = service.get(3L);

        assertThat(aliceResult.me().rank()).isEqualTo(1);
        assertThat(aliceResult.top()).extracting("rank").containsExactly(1, 1, 3);
        assertThat(carolResult.me().rank()).isEqualTo(3);
    }
}
