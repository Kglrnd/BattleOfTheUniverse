package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanetResourceRepository extends JpaRepository<PlanetResource, Long> {
    List<PlanetResource> findByPlanetId(Long planetId);

    Optional<PlanetResource> findByPlanetIdAndResourceKey(Long planetId, ResourceKey resourceKey);
}
