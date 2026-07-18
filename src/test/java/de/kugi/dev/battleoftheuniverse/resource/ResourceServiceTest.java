package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    void creditAddsTheAmountToEachLedgerRowAtomically() {
        when(repository.creditAmount(any(), any(), anyLong())).thenReturn(1);

        service.credit(PLANET_ID, new ResourceCost(200, 50, 10));

        verify(repository).creditAmount(PLANET_ID, ResourceKey.METAL, 200);
        verify(repository).creditAmount(PLANET_ID, ResourceKey.CRYSTAL, 50);
        verify(repository).creditAmount(PLANET_ID, ResourceKey.DEUTERIUM, 10);
    }

    @Test
    void debitSingleResourceSubtractsWhenAffordable() {
        when(repository.debitIfSufficient(PLANET_ID, ResourceKey.HYDROGEN, 40)).thenReturn(1);

        service.debit(PLANET_ID, ResourceKey.HYDROGEN, 40);

        verify(repository).debitIfSufficient(PLANET_ID, ResourceKey.HYDROGEN, 40);
    }

    @Test
    void debitSingleResourceThrowsWhenInsufficient() {
        when(repository.debitIfSufficient(PLANET_ID, ResourceKey.HYDROGEN, 40)).thenReturn(0);

        assertThatThrownBy(() -> service.debit(PLANET_ID, ResourceKey.HYDROGEN, 40))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Hydrogen");
    }

    @Test
    void creditSingleResourceAddsTheAmountAtomically() {
        when(repository.creditAmount(PLANET_ID, ResourceKey.HYDROGEN, 25)).thenReturn(1);

        service.credit(PLANET_ID, ResourceKey.HYDROGEN, 25);

        verify(repository).creditAmount(PLANET_ID, ResourceKey.HYDROGEN, 25);
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
    void applyProductionCreditsWholeUnitsAndCarriesOverTheFractionalRemainderAtomically() {
        // 30/hour for exactly 1 hour = 30 whole units, lastUpdate advances by exactly 1 hour.
        Instant start = Instant.now().minusSeconds(3600);
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        metal.setLastUpdate(start);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));

        service.applyProduction(PLANET_ID, ResourceKey.METAL, 30.0);

        ArgumentCaptor<Instant> newLastUpdateCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).applyProductionDelta(eq(PLANET_ID), eq(ResourceKey.METAL), eq(30L), eq(start), newLastUpdateCaptor.capture());
        assertThat(newLastUpdateCaptor.getValue()).isCloseTo(start.plusSeconds(3600), within(50, java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void applyProductionDoesNothingWhenLessThanAWholeUnitHasAccrued() {
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        metal.setLastUpdate(Instant.now());
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));

        service.applyProduction(PLANET_ID, ResourceKey.METAL, 30.0);

        verify(repository, never()).applyProductionDelta(any(), any(), anyLong(), any(), any());
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
        when(repository.debitIfSufficient(any(), any(), anyLong())).thenReturn(1);

        service.debit(PLANET_ID, new ResourceCost(200, 50, 10));

        verify(repository).debitIfSufficient(PLANET_ID, ResourceKey.METAL, 200);
        verify(repository).debitIfSufficient(PLANET_ID, ResourceKey.CRYSTAL, 50);
        verify(repository).debitIfSufficient(PLANET_ID, ResourceKey.DEUTERIUM, 10);
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
        verify(repository, never()).debitIfSufficient(any(), any(), anyLong());
    }

    @Test
    void debitCostFailsCleanlyWhenAConcurrentDebitWinsTheRaceAfterTheUpfrontCheck() {
        // The upfront check sees enough metal, but the atomic update itself finds it already spent
        // by a concurrent debit - simulating the exact race this atomic approach exists to catch.
        PlanetResource metal = new PlanetResource(PLANET_ID, ResourceKey.METAL, 1000);
        PlanetResource crystal = new PlanetResource(PLANET_ID, ResourceKey.CRYSTAL, 500);
        PlanetResource deuterium = new PlanetResource(PLANET_ID, ResourceKey.DEUTERIUM, 100);
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.METAL)).thenReturn(Optional.of(metal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.CRYSTAL)).thenReturn(Optional.of(crystal));
        when(repository.findByPlanetIdAndResourceKey(PLANET_ID, ResourceKey.DEUTERIUM)).thenReturn(Optional.of(deuterium));
        when(repository.debitIfSufficient(PLANET_ID, ResourceKey.METAL, 200)).thenReturn(0);

        assertThatThrownBy(() -> service.debit(PLANET_ID, new ResourceCost(200, 50, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not enough resources");
    }
}
