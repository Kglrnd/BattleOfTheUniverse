package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
