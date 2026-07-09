package de.kugi.dev.battleoftheuniverse.catalog;

import java.util.Arrays;

/**
 * The catalog data types that are seeded from {@code classpath:catalog/*.json}, copied
 * to a writable file on first startup, and editable by admins via
 * {@link AdminCatalogController} using a schema generated from the record type.
 */
public enum CatalogType {
    BUILDINGS(BuildingCatalog.class, "buildings.json"),
    SHIPS(ShipCatalog.class, "ships.json"),
    TECHNOLOGIES(TechnologyCatalog.class, "technologies.json"),
    DEFENSES(DefenseCatalog.class, "defenses.json");

    private final Class<?> catalogClass;
    private final String fileName;

    CatalogType(Class<?> catalogClass, String fileName) {
        this.catalogClass = catalogClass;
        this.fileName = fileName;
    }

    public Class<?> catalogClass() {
        return catalogClass;
    }

    public String fileName() {
        return fileName;
    }

    public static CatalogType fromKey(String key) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Unknown catalog type: " + key));
    }
}
