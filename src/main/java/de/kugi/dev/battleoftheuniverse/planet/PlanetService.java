package de.kugi.dev.battleoftheuniverse.planet;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
public class PlanetService {

    private static final int GALAXY_COUNT = 5;
    private static final int SYSTEMS_PER_GALAXY = 100;
    private static final int POSITIONS_PER_SYSTEM = 15;

    private final PlanetRepository planetRepository;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public PlanetService(PlanetRepository planetRepository, ApplicationEventPublisher events) {
        this.planetRepository = planetRepository;
        this.events = events;
    }

    @Transactional
    public Planet createStarterPlanet(Long ownerId, String ownerUsername) {
        int galaxy;
        int system;
        int position;
        do {
            galaxy = 1 + random.nextInt(GALAXY_COUNT);
            system = 1 + random.nextInt(SYSTEMS_PER_GALAXY);
            position = 1 + random.nextInt(POSITIONS_PER_SYSTEM);
        } while (planetRepository.existsByGalaxyAndSystemAndPosition(galaxy, system, position));

        Planet planet = new Planet(ownerUsername + "'s Homeworld", ownerId, galaxy, system, position, PlanetClass.TEMPERATE);
        planet = planetRepository.save(planet);

        events.publishEvent(new PlanetCreated(planet.getId(), ownerId));
        return planet;
    }

    public List<Planet> listMine(Long ownerId) {
        return planetRepository.findByOwnerId(ownerId);
    }

    public Planet getOwned(Long planetId, Long ownerId) {
        return planetRepository.findByIdAndOwnerId(planetId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found"));
    }

    /** Used by other modules that only need to verify ownership, not the full entity. */
    public boolean isOwnedBy(Long planetId, Long ownerId) {
        return planetRepository.findByIdAndOwnerId(planetId, ownerId).isPresent();
    }
}
