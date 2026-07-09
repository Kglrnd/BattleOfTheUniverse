package de.kugi.dev.battleoftheuniverse.defense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DefenseJobRepository extends JpaRepository<DefenseJob, Long> {
    Optional<DefenseJob> findByPlanetId(Long planetId);

    List<DefenseJob> findByEndsAtBefore(Instant instant);
}
