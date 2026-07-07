package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "planet_resources", uniqueConstraints = @UniqueConstraint(columnNames = {"planet_id", "resource_key"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanetResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    @Enumerated(EnumType.STRING)
    private ResourceKey resourceKey;

    private long amount;

    private Instant lastUpdate = Instant.now();

    public PlanetResource(Long planetId, ResourceKey resourceKey, long amount) {
        this.planetId = planetId;
        this.resourceKey = resourceKey;
        this.amount = amount;
    }
}
