package de.kugi.dev.battleoftheuniverse.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceCostTest {

    @Test
    void scalesAllComponentsAndRounds() {
        ResourceCost base = new ResourceCost(60, 15, 0);

        ResourceCost scaled = base.scaled(Math.pow(1.5, 2));

        assertThat(scaled.metal()).isEqualTo(135);
        assertThat(scaled.crystal()).isEqualTo(34);
        assertThat(scaled.deuterium()).isEqualTo(0);
    }

    @Test
    void affordableOnlyWhenAllThreeResourcesSuffice() {
        ResourceCost cost = new ResourceCost(100, 50, 10);

        assertThat(cost.isAffordable(100, 50, 10)).isTrue();
        assertThat(cost.isAffordable(99, 50, 10)).isFalse();
        assertThat(cost.isAffordable(100, 50, 9)).isFalse();
    }
}
