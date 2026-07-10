package de.kugi.dev.battleoftheuniverse.planet;

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

/**
 * A planet is referenced from other modules purely by its {@code id} / {@code ownerId}
 * (plain {@code Long}s, no JPA relationship) so that {@code resource} and
 * {@code building} can depend on {@code planet} without any risk of a reverse
 * dependency ever being needed.
 */
@Entity
@Table(name = "planets", uniqueConstraints = @UniqueConstraint(columnNames = {"galaxy", "system", "position"}))
@Getter
@Setter
@NoArgsConstructor
public class Planet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long ownerId;

    private int galaxy;
    private int system;
    private int position;

    @Enumerated(EnumType.STRING)
    private PlanetClass planetClass = PlanetClass.TEMPERATE;

    private Instant createdAt = Instant.now();

    private boolean homeworld = false;

    /** Fixed at creation, 85.00-109.99; see {@link de.kugi.dev.battleoftheuniverse.research.ResearchService}. */
    private double researchEfficiency = 100.0;

    private boolean researchPlanet = false;

    public Planet(String name, Long ownerId, int galaxy, int system, int position, PlanetClass planetClass) {
        this.name = name;
        this.ownerId = ownerId;
        this.galaxy = galaxy;
        this.system = system;
        this.position = position;
        this.planetClass = planetClass;
    }

    public String getCoordinates() {
        return "[%d:%d:%d]".formatted(galaxy, system, position);
    }
}
