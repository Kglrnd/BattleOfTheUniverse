package de.kugi.dev.battleoftheuniverse.building;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "planet_buildings", uniqueConstraints = @UniqueConstraint(columnNames = {"planet_id", "building_key"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanetBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String buildingKey;

    private int level;

    public PlanetBuilding(Long planetId, String buildingKey, int level) {
        this.planetId = planetId;
        this.buildingKey = buildingKey;
        this.level = level;
    }
}
