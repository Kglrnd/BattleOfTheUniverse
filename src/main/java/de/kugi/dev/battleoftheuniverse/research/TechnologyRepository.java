package de.kugi.dev.battleoftheuniverse.research;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {
    List<Technology> findByUserId(Long userId);

    Optional<Technology> findByUserIdAndTechnologyKey(Long userId, String technologyKey);
}
