package de.kugi.dev.battleoftheuniverse.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * Admin-only editor backend for the game-balance catalog. Every gameplay screen is
 * hand-built Angular; this is the one deliberately schema-driven surface, rendered
 * client-side by JSON Forms from the schema returned here.
 */
@RestController
@RequestMapping("/api/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogService catalogService;

    @GetMapping("/{type}")
    public JsonNode data(@PathVariable String type) {
        return catalogService.currentData(CatalogType.fromKey(type));
    }

    @GetMapping("/{type}/schema")
    public JsonNode schema(@PathVariable String type) {
        return catalogService.schemaFor(CatalogType.fromKey(type));
    }

    @PutMapping("/{type}")
    public JsonNode save(@PathVariable String type, @RequestBody JsonNode body) {
        CatalogType catalogType = CatalogType.fromKey(type);
        catalogService.save(catalogType, body);
        return catalogService.currentData(catalogType);
    }
}
