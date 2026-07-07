package de.kugi.dev.battleoftheuniverse.research;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A researched technology level for a user, built up via {@link ResearchJob}/
 * {@link ResearchService}. Research is account-wide rather than per-planet, matching
 * how a single research lab queue benefits every colony.
 */
@Entity
@Table(name = "technologies")
@Getter
@Setter
@NoArgsConstructor
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String technologyKey;

    private int level;

    public Technology(Long userId, String technologyKey, int level) {
        this.userId = userId;
        this.technologyKey = technologyKey;
        this.level = level;
    }
}
