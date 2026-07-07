package de.kugi.dev.battleoftheuniverse.catalog;

import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates JSON Schema (Draft 7) from the catalog record types with victools, so the
 * schema handed to the Angular JSON Forms editor can never drift from the actual Java
 * shape — the record is the single source of truth.
 */
@Service
public class JsonSchemaService {

    private final SchemaGenerator generator;
    private final Map<Class<?>, JsonNode> cache = new ConcurrentHashMap<>();

    public JsonSchemaService() {
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.build();
        this.generator = new SchemaGenerator(config);
    }

    public JsonNode schemaFor(Class<?> type) {
        return cache.computeIfAbsent(type, generator::generateSchema);
    }
}
