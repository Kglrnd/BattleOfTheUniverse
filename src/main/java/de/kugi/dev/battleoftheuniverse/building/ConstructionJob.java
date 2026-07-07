package de.kugi.dev.battleoftheuniverse.building;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "construction_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ConstructionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String buildingKey;

    private int targetLevel;

    private Instant startedAt;

    private Instant endsAt;

    public ConstructionJob(Long planetId, String buildingKey, int targetLevel, Instant startedAt, Instant endsAt) {
        this.planetId = planetId;
        this.buildingKey = buildingKey;
        this.targetLevel = targetLevel;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }
}
