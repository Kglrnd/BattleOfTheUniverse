package de.kugi.dev.battleoftheuniverse.fleet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A stationed ship stack on a planet. Built up via {@link ShipyardJob}, drawn down
 * when dispatched on a {@link FleetMovement} (see {@link FleetService}).
 */
@Entity
@Table(name = "ships")
@Getter
@Setter
@NoArgsConstructor
public class Ship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String shipKey;

    private int quantity;

    public Ship(Long planetId, String shipKey, int quantity) {
        this.planetId = planetId;
        this.shipKey = shipKey;
        this.quantity = quantity;
    }
}
