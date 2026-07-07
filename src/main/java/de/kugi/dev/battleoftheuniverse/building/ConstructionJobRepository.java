package de.kugi.dev.battleoftheuniverse.building;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConstructionJobRepository extends JpaRepository<ConstructionJob, Long> {
    Optional<ConstructionJob> findByPlanetId(Long planetId);

    List<ConstructionJob> findByEndsAtBefore(Instant instant);
}
