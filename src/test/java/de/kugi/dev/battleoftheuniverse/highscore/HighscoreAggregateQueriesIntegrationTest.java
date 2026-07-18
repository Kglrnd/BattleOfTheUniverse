package de.kugi.dev.battleoftheuniverse.highscore;

import de.kugi.dev.battleoftheuniverse.building.PlanetBuilding;
import de.kugi.dev.battleoftheuniverse.building.PlanetBuildingRepository;
import de.kugi.dev.battleoftheuniverse.defense.Tower;
import de.kugi.dev.battleoftheuniverse.defense.TowerRepository;
import de.kugi.dev.battleoftheuniverse.fleet.Ship;
import de.kugi.dev.battleoftheuniverse.fleet.ShipRepository;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetClass;
import de.kugi.dev.battleoftheuniverse.planet.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the raw JPQL behind the highscore's grouped-sum queries against a real (H2)
 * database - a Mockito-based {@code HighscoreServiceTest} can't catch a query syntax mistake
 * since the repository itself is mocked there.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HighscoreAggregateQueriesIntegrationTest {

    @Autowired
    private PlanetRepository planetRepository;
    @Autowired
    private PlanetBuildingRepository planetBuildingRepository;
    @Autowired
    private ShipRepository shipRepository;
    @Autowired
    private TowerRepository towerRepository;

    @Test
    void sumsBuildingLevelsShipsAndTowersPerOwnerAcrossMultiplePlanets() {
        Planet aliceHome = planetRepository.save(new Planet("Alice Home", 100L, 1, 1, 1, PlanetClass.TEMPERATE));
        Planet aliceColony = planetRepository.save(new Planet("Alice Colony", 100L, 1, 1, 2, PlanetClass.TEMPERATE));
        Planet bobHome = planetRepository.save(new Planet("Bob Home", 200L, 1, 1, 3, PlanetClass.TEMPERATE));

        planetBuildingRepository.save(new PlanetBuilding(aliceHome.getId(), "metal_mine", 3));
        planetBuildingRepository.save(new PlanetBuilding(aliceColony.getId(), "metal_mine", 2));
        planetBuildingRepository.save(new PlanetBuilding(bobHome.getId(), "metal_mine", 4));

        shipRepository.save(new Ship(aliceHome.getId(), "cruiser", 5));
        shipRepository.save(new Ship(aliceColony.getId(), "cruiser", 1));

        towerRepository.save(new Tower(bobHome.getId(), "light_defense_tower", 7));

        assertThat(planetRepository.findDistinctOwnerIds()).contains(100L, 200L);

        var buildingTotals = planetBuildingRepository.sumLevelsByOwnerAndBuildingKey();
        assertThat(buildingTotals)
                .anySatisfy(row -> {
                    assertThat(row.getOwnerId()).isEqualTo(100L);
                    assertThat(row.getKey()).isEqualTo("metal_mine");
                    assertThat(row.getTotal()).isEqualTo(5L);
                })
                .anySatisfy(row -> {
                    assertThat(row.getOwnerId()).isEqualTo(200L);
                    assertThat(row.getKey()).isEqualTo("metal_mine");
                    assertThat(row.getTotal()).isEqualTo(4L);
                });

        var shipTotals = shipRepository.sumQuantitiesByOwnerAndShipKey();
        assertThat(shipTotals).anySatisfy(row -> {
            assertThat(row.getOwnerId()).isEqualTo(100L);
            assertThat(row.getKey()).isEqualTo("cruiser");
            assertThat(row.getTotal()).isEqualTo(6L);
        });

        var towerTotals = towerRepository.sumQuantitiesByOwnerAndTowerKey();
        assertThat(towerTotals).anySatisfy(row -> {
            assertThat(row.getOwnerId()).isEqualTo(200L);
            assertThat(row.getKey()).isEqualTo("light_defense_tower");
            assertThat(row.getTotal()).isEqualTo(7L);
        });
    }
}
