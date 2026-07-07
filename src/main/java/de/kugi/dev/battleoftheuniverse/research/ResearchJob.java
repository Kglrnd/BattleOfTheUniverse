package de.kugi.dev.battleoftheuniverse.research;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Research is account-wide (one lab queue per user, see {@link Technology}), but
 * paid for out of a specific planet's stockpile — {@code planetId} is kept only so
 * the origin can be shown back to the player, it plays no role once the job is queued.
 */
@Entity
@Table(name = "research_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ResearchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long planetId;

    private String technologyKey;

    private int targetLevel;

    private Instant startedAt;

    private Instant endsAt;

    public ResearchJob(Long userId, Long planetId, String technologyKey, int targetLevel, Instant startedAt, Instant endsAt) {
        this.userId = userId;
        this.planetId = planetId;
        this.technologyKey = technologyKey;
        this.targetLevel = targetLevel;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }
}
