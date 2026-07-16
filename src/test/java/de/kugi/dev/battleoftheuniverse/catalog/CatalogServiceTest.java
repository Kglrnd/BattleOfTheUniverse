package de.kugi.dev.battleoftheuniverse.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Points {@code catalogDir} at a throwaway @TempDir instead of the real ./config/catalog a dev server also uses. */
class CatalogServiceTest {

    @TempDir
    private Path catalogDir;

    private CatalogService service;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        service = new CatalogService(new JsonSchemaService());
        ReflectionTestUtils.setField(service, "catalogDirPath", catalogDir.toString());
        service.loadAll();
    }

    @Test
    void seedsEveryCatalogTypeFromClasspathDefaultsOnFirstLoad() {
        assertThat(service.buildings()).isNotEmpty();
        assertThat(service.ships()).isNotEmpty();
        assertThat(service.technologies()).isNotEmpty();
        assertThat(service.defenses()).isNotEmpty();
        for (CatalogType type : CatalogType.values()) {
            assertThat(catalogDir.resolve(type.fileName())).exists();
        }
    }

    @Test
    void buildingLooksUpByKey() {
        BuildingDefinition first = service.buildings().get(0);

        assertThat(service.building(first.key())).isEqualTo(first);
    }

    @Test
    void buildingThrowsForAnUnknownKey() {
        assertThatThrownBy(() -> service.building("does_not_exist"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unknown building");
    }

    @Test
    void shipThrowsForAnUnknownKey() {
        assertThatThrownBy(() -> service.ship("does_not_exist")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void technologyThrowsForAnUnknownKey() {
        assertThatThrownBy(() -> service.technology("does_not_exist")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void defenseThrowsForAnUnknownKey() {
        assertThatThrownBy(() -> service.defense("does_not_exist")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void costForABuildingScalesExponentiallyByCostGrowthFactor() {
        BuildingDefinition definition = new BuildingDefinition("k", "n", "d",
                new ResourceCost(100, 50, 0), 1.5, 60, ResourceKey.NONE, 0, 1, List.of());

        assertThat(service.costFor(definition, 1)).isEqualTo(new ResourceCost(100, 50, 0));
        // Level 2: 100 * 1.5^1 = 150
        assertThat(service.costFor(definition, 2).metal()).isEqualTo(150);
    }

    @Test
    void buildTimeForABuildingScalesLinearlyWithTargetLevel() {
        BuildingDefinition definition = new BuildingDefinition("k", "n", "d",
                ResourceCost.ZERO, 1.5, 60, ResourceKey.NONE, 0, 1, List.of());

        assertThat(service.buildTimeFor(definition, 3)).isEqualTo(Duration.ofSeconds(180));
    }

    @Test
    void productionPerHourScalesLinearlyWithLevel() {
        BuildingDefinition definition = new BuildingDefinition("k", "n", "d",
                ResourceCost.ZERO, 1.5, 60, ResourceKey.METAL, 30, 1, List.of());

        assertThat(service.productionPerHour(definition, 5)).isEqualTo(150.0);
    }

    @Test
    void schemaForACatalogTypeDelegatesToJsonSchemaService() {
        JsonNode schema = service.schemaFor(CatalogType.BUILDINGS);

        assertThat(schema.get("type").asString()).isEqualTo("object");
    }

    @Test
    void saveWritesValidDataToDiskAndReloadsInMemoryState() throws IOException {
        JsonNode current = service.currentData(CatalogType.BUILDINGS);

        service.save(CatalogType.BUILDINGS, current);

        assertThat(catalogDir.resolve("buildings.json")).exists();
        assertThat(service.buildings()).isNotEmpty();
    }

    @Test
    void saveRejectsDataThatFailsSchemaValidation() {
        JsonNode invalid = jsonMapper.readTree("{\"buildings\": [{\"key\": \"broken\"}]}");

        assertThatThrownBy(() -> service.save(CatalogType.BUILDINGS, invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid catalog data");
    }

    @Test
    void existingLiveConfigFileIsNeverOverwrittenByClasspathDefaultsOnReload() throws IOException {
        // Simulate a previously-saved admin edit (a renamed building) already on disk, then reload.
        Path liveFile = catalogDir.resolve("buildings.json");
        String original = Files.readString(liveFile);
        String edited = original.replaceFirst("\"name\"\\s*:\\s*\"[^\"]*\"", "\"name\": \"Admin Renamed\"");
        assertThat(edited).isNotEqualTo(original);
        Files.writeString(liveFile, edited);

        service.loadAll();

        assertThat(Files.readString(liveFile)).contains("Admin Renamed");
        assertThat(service.buildings()).anySatisfy(b -> assertThat(b.name()).isEqualTo("Admin Renamed"));
    }
}
