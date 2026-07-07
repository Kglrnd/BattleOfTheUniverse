package de.kugi.dev.battleoftheuniverse.fleet;

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
@Table(name = "shipyard_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ShipyardJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String shipKey;

    private int quantity;

    private Instant startedAt;

    private Instant endsAt;

    public ShipyardJob(Long planetId, String shipKey, int quantity, Instant startedAt, Instant endsAt) {
        this.planetId = planetId;
        this.shipKey = shipKey;
        this.quantity = quantity;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }
}
