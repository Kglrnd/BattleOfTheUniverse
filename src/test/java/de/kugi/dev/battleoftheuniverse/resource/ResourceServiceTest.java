package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
