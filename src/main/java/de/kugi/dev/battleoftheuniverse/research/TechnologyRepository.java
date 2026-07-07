package de.kugi.dev.battleoftheuniverse.research;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {
    List<Technology> findByUserId(Long userId);
}
