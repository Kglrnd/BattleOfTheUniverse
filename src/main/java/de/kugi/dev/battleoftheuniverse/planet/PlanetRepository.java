package de.kugi.dev.battleoftheuniverse.planet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanetRepository extends JpaRepository<Planet, Long> {
    List<Planet> findByOwnerId(Long ownerId);

    boolean existsByGalaxyAndSystemAndPosition(int galaxy, int system, int position);

    Optional<Planet> findByIdAndOwnerId(Long id, Long ownerId);
}
