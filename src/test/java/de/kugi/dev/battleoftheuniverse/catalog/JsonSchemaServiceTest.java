package de.kugi.dev.battleoftheuniverse.catalog;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaServiceTest {

    private final JsonSchemaService service = new JsonSchemaService();

    @Test
    void generatesSchemaWithArrayOfBuildingDefinitions() {
        JsonNode schema = service.schemaFor(BuildingCatalog.class);

        assertThat(schema.get("type").asString()).isEqualTo("object");
        JsonNode buildingsProperty = schema.get("properties").get("buildings");
        assertThat(buildingsProperty).isNotNull();
        assertThat(buildingsProperty.get("type").asString()).isEqualTo("array");
    }

    @Test
    void cachesGeneratedSchemaPerType() {
        JsonNode first = service.schemaFor(BuildingCatalog.class);
        JsonNode second = service.schemaFor(BuildingCatalog.class);

        assertThat(first).isSameAs(second);
    }
}
