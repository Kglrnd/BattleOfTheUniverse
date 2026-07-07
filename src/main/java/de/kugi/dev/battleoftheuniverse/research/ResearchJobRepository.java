package de.kugi.dev.battleoftheuniverse.research;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ResearchJobRepository extends JpaRepository<ResearchJob, Long> {
    Optional<ResearchJob> findByUserId(Long userId);

    List<ResearchJob> findByEndsAtBefore(Instant instant);
}
