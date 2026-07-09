package de.kugi.dev.battleoftheuniverse.defense;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A stationed defense tower stack on a planet. Built up via {@link DefenseJob}, drawn
 * down by {@code combat} when the planet is attacked - towers never leave the planet.
 */
@Entity
@Table(name = "towers")
@Getter
@Setter
@NoArgsConstructor
public class Tower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planetId;

    private String towerKey;

    private int quantity;

    public Tower(Long planetId, String towerKey, int quantity) {
        this.planetId = planetId;
        this.towerKey = towerKey;
        this.quantity = quantity;
    }
}
