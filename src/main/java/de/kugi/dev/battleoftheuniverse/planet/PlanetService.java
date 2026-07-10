package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.AdminPlanetView;
import de.kugi.dev.battleoftheuniverse.planet.dto.PlanetMapper;
import de.kugi.dev.battleoftheuniverse.planet.dto.SystemSlotView;
import de.kugi.dev.battleoftheuniverse.planet.dto.SystemView;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanetService {

    private final PlanetRepository planetRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final PlanetMapper planetMapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Planet createStarterPlanet(Long ownerId, String ownerUsername) {
        return createPlanet(ownerId, ownerUsername + "'s Homeworld", true);
    }

    @Transactional
    public Planet createColonyPlanet(Long ownerId, String name) {
        return createPlanet(ownerId, name, false);
    }

    /** Founds a colony at an exact, previously-scouted position - used when a colonize mission arrives. */
    @Transactional
    public Planet createColonyPlanetAt(Long ownerId, String name, int galaxy, int system, int position) {
        if (!isColonizable(galaxy, system, position)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target position can no longer be colonized");
        }
        return place(ownerId, name, galaxy, system, position, false);
    }

    /** In bounds, on a usable slot, and not already occupied. */
    public boolean isColonizable(int galaxy, int system, int position) {
        return SystemLayout.isInBounds(galaxy, system)
                && SystemLayout.usablePositions(galaxy, system).contains(position)
                && planetRepository.findByGalaxyAndSystemAndPosition(galaxy, system, position).isEmpty();
    }

    public Optional<Planet> findAtPosition(int galaxy, int system, int position) {
        return planetRepository.findByGalaxyAndSystemAndPosition(galaxy, system, position);
    }

    private Planet createPlanet(Long ownerId, String name, boolean homeworld) {
        int[] pos = randomFreePosition();
        return place(ownerId, name, pos[0], pos[1], pos[2], homeworld);
    }

    private int[] randomFreePosition() {
        int galaxy;
        int system;
        int position;
        do {
            galaxy = 1 + random.nextInt(SystemLayout.GALAXY_COUNT);
            system = 1 + random.nextInt(SystemLayout.SYSTEMS_PER_GALAXY);
            List<Integer> usable = List.copyOf(SystemLayout.usablePositions(galaxy, system));
            position = usable.get(random.nextInt(usable.size()));
        } while (planetRepository.existsByGalaxyAndSystemAndPosition(galaxy, system, position));

        return new int[]{galaxy, system, position};
    }

    private Planet place(Long ownerId, String name, int galaxy, int system, int position, boolean homeworld) {
        Planet planet = new Planet(name, ownerId, galaxy, system, position, PlanetClass.TEMPERATE);
        planet.setHomeworld(homeworld);
        planet.setResearchEfficiency(rollResearchEfficiency());
        planet = planetRepository.save(planet);

        events.publishEvent(new PlanetCreated(planet.getId(), ownerId));
        return planet;
    }

    /** Uniform roll in [85.00, 109.99], fixed for the planet's lifetime. */
    private double rollResearchEfficiency() {
        int hundredths = 8500 + random.nextInt(10999 - 8500 + 1);
        return hundredths / 100.0;
    }

    public List<Planet> listMine(Long ownerId) {
        return planetRepository.findByOwnerId(ownerId);
    }

    /**
     * Admin-triggered game reset: deletes every colony, leaving only homeworlds, and moves
     * each surviving homeworld to a fresh random position. Returns the repositioned
     * homeworlds so the caller can reseed their starter building/resource state.
     */
    @Transactional
    public List<Planet> resetAllToHomeworldsOnly() {
        List<Planet> all = planetRepository.findAll();
        planetRepository.deleteAll(all.stream().filter(p -> !p.isHomeworld()).toList());

        List<Planet> updated = new ArrayList<>();
        for (Planet homeworld : all.stream().filter(Planet::isHomeworld).toList()) {
            int[] pos = randomFreePosition();
            homeworld.setGalaxy(pos[0]);
            homeworld.setSystem(pos[1]);
            homeworld.setPosition(pos[2]);
            updated.add(planetRepository.save(homeworld));
        }
        return updated;
    }

    /** Every planet's ID in the game - used by other modules for one-off backfills. */
    public List<Long> listAllIds() {
        return planetRepository.findAll().stream().map(Planet::getId).toList();
    }

    /** Every planet in the game, owner username resolved - admin-only, so no owner scoping. */
    public List<AdminPlanetView> listAllForAdmin() {
        List<Planet> planets = planetRepository.findAll();
        Set<Long> ownerIds = planets.stream().map(Planet::getOwnerId).collect(Collectors.toSet());
        Map<Long, String> usernamesByOwnerId = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return planets.stream()
                .map(planet -> planetMapper.toAdminView(planet, usernamesByOwnerId.getOrDefault(planet.getOwnerId(), "unknown")))
                .toList();
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

    public Optional<Planet> findActiveResearchPlanet(Long ownerId) {
        return planetRepository.findByOwnerIdAndResearchPlanetTrue(ownerId);
    }

    /** Switches the account's active research planet, clearing any previous one. */
    @Transactional
    public Planet activateResearchPlanet(Long ownerId, Long planetId) {
        Planet target = getOwned(planetId, ownerId);

        findActiveResearchPlanet(ownerId)
                .filter(previous -> !previous.getId().equals(target.getId()))
                .ifPresent(previous -> {
                    previous.setResearchPlanet(false);
                    planetRepository.save(previous);
                });

        target.setResearchPlanet(true);
        return planetRepository.save(target);
    }

    /** Unscoped lookup for cross-module callers that have already verified ownership themselves. */
    public Planet getById(Long planetId) {
        return planetRepository.findById(planetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found"));
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
                slots.add(SystemSlotView.occupied(position, planetMapper.toView(planet)));
            } else if (!usable.contains(position)) {
                slots.add(SystemSlotView.voidSlot(position));
            } else {
                slots.add(SystemSlotView.free(position));
            }
        }
        return new SystemView(galaxy, system, slots);
    }
}
