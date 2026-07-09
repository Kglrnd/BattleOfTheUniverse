package de.kugi.dev.battleoftheuniverse.defense;

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
@Table(name = "defense_jobs")
@Getter
@Setter
@NoArgsConstructor
public class DefenseJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String towerKey;

    private int quantity;

    private Instant startedAt;

    private Instant endsAt;

    public DefenseJob(Long planetId, String towerKey, int quantity, Instant startedAt, Instant endsAt) {
        this.planetId = planetId;
        this.towerKey = towerKey;
        this.quantity = quantity;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }
}
