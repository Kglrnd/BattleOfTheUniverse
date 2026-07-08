package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.SystemSlotView;
import de.kugi.dev.battleoftheuniverse.planet.dto.SystemView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlanetService {

    private final PlanetRepository planetRepository;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public PlanetService(PlanetRepository planetRepository, ApplicationEventPublisher events) {
        this.planetRepository = planetRepository;
        this.events = events;
    }

    @Transactional
    public Planet createStarterPlanet(Long ownerId, String ownerUsername) {
        return createPlanet(ownerId, ownerUsername + "'s Homeworld", true);
    }

    @Transactional
    public Planet createColonyPlanet(Long ownerId, String name) {
        return createPlanet(ownerId, name, false);
    }

    private Planet createPlanet(Long ownerId, String name, boolean homeworld) {
        int galaxy;
        int system;
        int position;
        do {
            galaxy = 1 + random.nextInt(SystemLayout.GALAXY_COUNT);
            system = 1 + random.nextInt(SystemLayout.SYSTEMS_PER_GALAXY);
            List<Integer> usable = List.copyOf(SystemLayout.usablePositions(galaxy, system));
            position = usable.get(random.nextInt(usable.size()));
        } while (planetRepository.existsByGalaxyAndSystemAndPosition(galaxy, system, position));

        Planet planet = new Planet(name, ownerId, galaxy, system, position, PlanetClass.TEMPERATE);
        planet.setHomeworld(homeworld);
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

    public Planet getHome(Long ownerId) {
        return planetRepository.findByOwnerIdAndHomeworldTrue(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Home planet not found"));
    }

    /** Used by other modules that only need to verify ownership, not the full entity. */
    public boolean isOwnedBy(Long planetId, Long ownerId) {
        return planetRepository.findByIdAndOwnerId(planetId, ownerId).isPresent();
    }

    public SystemView getSystemView(int galaxy, int system) {
        if (!SystemLayout.isInBounds(galaxy, system)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found");
        }

        Set<Integer> usable = SystemLayout.usablePositions(galaxy, system);
        Map<Integer, Planet> byPosition = planetRepository.findByGalaxyAndSystem(galaxy, system).stream()
                .collect(Collectors.toMap(Planet::getPosition, Function.identity()));

        List<SystemSlotView> slots = new ArrayList<>(SystemLayout.MAX_POSITIONS);
        for (int position = 1; position <= SystemLayout.MAX_POSITIONS; position++) {
            Planet planet = byPosition.get(position);
            if (planet != null) {
                // A planet actually sitting here always wins, even if the computed layout
                // doesn't consider the position usable (e.g. it predates a layout change).
                slots.add(SystemSlotView.occupied(position, planet));
            } else if (!usable.contains(position)) {
                slots.add(SystemSlotView.voidSlot(position));
            } else {
                slots.add(SystemSlotView.free(position));
            }
        }
        return new SystemView(galaxy, system, slots);
    }
}
