package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
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
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    private static final Long PLANET_ID = 1L;

    @Mock
    private PlanetResourceRepository repository;

    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(repository);
    }

    @Test
    void creditAddsTheAmountToEachLedgerRow() {
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        PlanetResource crystal = new PlanetResource(PLANET_ID, ResourceKey.CRYSTAL, 500);
        PlanetResource deuterium = new PlanetResource(PLANET_ID, ResourceKey.DEUTERIUM, 100);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.CRYSTAL)).thenReturn(Optional.of(crystal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.DEUTERIUM)).thenReturn(Optional.of(deuterium));

        service.credit(PLANET_ID, new ResourceCost(200, 50, 10));

        assertThat(metal.getAmount()).isEqualTo(1200);
        assertThat(crystal.getAmount()).isEqualTo(550);
        assertThat(deuterium.getAmount()).isEqualTo(110);
    }

    @Test
    void debitSingleResourceSubtractsWhenAffordable() {
        PlanetResource hydrogen = new PlanetResource(PLANET_ID, ResourceKey.HYDROGEN, 100);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.HYDROGEN)).thenReturn(Optional.of(hydrogen));

        service.debit(PLANET_ID, ResourceKey.HYDROGEN, 40);

        assertThat(hydrogen.getAmount()).isEqualTo(60);
    }

    @Test
    void debitSingleResourceThrowsWhenInsufficient() {
        PlanetResource hydrogen = new PlanetResource(PLANET_ID, ResourceKey.HYDROGEN, 10);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.HYDROGEN)).thenReturn(Optional.of(hydrogen));

        assertThatThrownBy(() -> service.debit(PLANET_ID, ResourceKey.HYDROGEN, 40))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Hydrogen");
        assertThat(hydrogen.getAmount()).isEqualTo(10);
    }

    @Test
    void creditSingleResourceAddsTheAmount() {
        PlanetResource hydrogen = new PlanetResource(PLANET_ID, ResourceKey.HYDROGEN, 10);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.HYDROGEN)).thenReturn(Optional.of(hydrogen));

        service.credit(PLANET_ID, ResourceKey.HYDROGEN, 25);

        assertThat(hydrogen.getAmount()).isEqualTo(35);
    }

    @Test
    void initializeStarterSeedsAllFiveLedgerRows() {
        service.initializeStarter(PLANET_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(PlanetResource.class);
        verify(repository, times(5)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(PlanetResource::getResourceKey)
                .containsExactlyInAnyOrder(ResourceKey.METAL, ResourceKey.CRYSTAL, ResourceKey.DEUTERIUM,
                        ResourceKey.HYDROGEN, ResourceKey.ENERGY);
    }

    @Test
    void rawReturnsEveryLedgerRowForThePlanet() {
        List<PlanetResource> rows = List.of(new PlanetResource(PLANET_ID, ResourceKey.METAL, 100));
        when(repository.findByPlanetId(PLANET_ID)).thenReturn(rows);

        assertThat(service.raw(PLANET_ID)).isEqualTo(rows);
    }

    @Test
    void wipeAllDeletesEveryLedgerRow() {
        service.wipeAll();

        verify(repository).deleteAll();
    }

    @Test
    void deleteAllForPlanetDeletesOnlyThatPlanetsRows() {
        List<PlanetResource> rows = List.of(new PlanetResource(PLANET_ID, ResourceKey.METAL, 100));
        when(repository.findByPlanetId(PLANET_ID)).thenReturn(rows);

        service.deleteAllForPlanet(PLANET_ID);

        verify(repository).deleteAll(rows);
    }

    @Test
    void applyProductionCreditsWholeUnitsAndCarriesOverTheFractionalRemainder() {
        // 30/hour for exactly 1 hour = 30 whole units, lastUpdate advances by exactly 1 hour.
        Instant start = Instant.now().minusSeconds(3600);
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        metal.setLastUpdate(start);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));

        service.applyProduction(PLANET_ID, ResourceKey.METAL, 30.0);

        assertThat(metal.getAmount()).isEqualTo(1030);
        assertThat(metal.getLastUpdate()).isCloseTo(start.plusSeconds(3600), within(50, java.time.temporal.ChronoUnit.MILLIS));
        verify(repository).save(metal);
    }

    @Test
    void applyProductionDoesNothingWhenLessThanAWholeUnitHasAccrued() {
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        metal.setLastUpdate(Instant.now());
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));

        service.applyProduction(PLANET_ID, ResourceKey.METAL, 30.0);

        assertThat(metal.getAmount()).isEqualTo(1000);
        verify(repository, never()).save(any());
    }

    @Test
    void applyProductionIsANoOpForAZeroOrNegativeRate() {
        service.applyProduction(PLANET_ID, ResourceKey.METAL, 0.0);
        service.applyProduction(PLANET_ID, ResourceKey.METAL, -5.0);

        verify(repository, never()).findByPlanetIdAndResourceKey(any(), any());
    }

    @Test
    void debitCostSubtractsFromAllThreeLedgersWhenAffordable() {
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        PlanetResource crystal = new PlanetResource(PLANET_ID, ResourceKey.CRYSTAL, 500);
        PlanetResource deuterium = new PlanetResource(PLANET_ID, ResourceKey.DEUTERIUM, 100);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.CRYSTAL)).thenReturn(Optional.of(crystal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.DEUTERIUM)).thenReturn(Optional.of(deuterium));

        service.debit(PLANET_ID, new ResourceCost(200, 50, 10));

        assertThat(metal.getAmount()).isEqualTo(800);
        assertThat(crystal.getAmount()).isEqualTo(450);
        assertThat(deuterium.getAmount()).isEqualTo(90);
    }

    @Test
    void debitCostThrowsAndChangesNothingWhenNotAffordable() {
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 100);
        PlanetResource crystal = new PlanetResource(PLANET_ID, ResourceKey.CRYSTAL, 500);
        PlanetResource deuterium = new PlanetResource(PLANET_ID, ResourceKey.DEUTERIUM, 100);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.CRYSTAL)).thenReturn(Optional.of(crystal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.DEUTERIUM)).thenReturn(Optional.of(deuterium));

        assertThatThrownBy(() -> service.debit(PLANET_ID, new ResourceCost(200, 50, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not enough resources");
        assertThat(metal.getAmount()).isEqualTo(100);
        verify(repository, never()).save(any());
    }
}
