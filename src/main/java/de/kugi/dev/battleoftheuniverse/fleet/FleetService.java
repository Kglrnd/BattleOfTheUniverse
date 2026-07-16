package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.Requirement;
import de.kugi.dev.battleoftheuniverse.catalog.RequirementType;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionsRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementMapper;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.IncomingMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.LockedRequirement;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ResourceQuantity;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardBuildResponse;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardView;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.research.dto.DriveOption;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceMapper;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceView;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FleetService {

    /**
     * Rough distance units per galaxy/system/position step. Picked so a same-system
     * hop takes seconds and a cross-galaxy jump takes tens of seconds to a few minutes
     * at typical ship/drive speeds — tuned for a playable dev tempo, not realism.
     */
    private static final long GALAXY_STEP = 20_000;
    private static final long SYSTEM_STEP = 1_000;
    private static final long POSITION_STEP = 100;

    /** Matches the "shipyard" entry in the building catalog; its level speeds up ship construction. */
    private static final String SHIPYARD_KEY = "shipyard";

    /** Only this ship can found a colony - matches the "colony_ship" entry in the ship catalog. */
    private static final String COLONY_SHIP_KEY = "colony_ship";

    /** Only this ship can run an espionage mission - matches the "espionage_probe" catalog entry. */
    private static final String PROBE_SHIP_KEY = "espionage_probe";

    /** Mandatory escort for BOMBARD/INVADE - matches the "galaxy_class" catalog entry. */
    private static final String GALAXY_CLASS_KEY = "galaxy_class";

    /** Only this ship can bombard a planet - matches the "orbital_bomb" catalog entry. */
    private static final String BOMB_KEY = "orbital_bomb";

    /** Only this ship can invade a planet - matches the "invasion_unit" catalog entry. */
    private static final String INVASION_KEY = "invasion_unit";

    /** Espionage success chance at technology level 0, before any per-level bonus. */
    private static final double ESPIONAGE_BASE_CHANCE = 0.5;
    private static final double ESPIONAGE_CHANCE_PER_LEVEL = 0.05;
    private static final double ESPIONAGE_MAX_CHANCE = 0.95;

    private final ShipRepository shipRepository;
    private final ShipyardJobRepository jobRepository;
    private final FleetMovementRepository movementRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final ResearchService researchService;
    private final BuildingService buildingService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final FleetMovementMapper fleetMovementMapper;
    private final ResourceMapper resourceMapper;

    public List<ShipyardView> listForPlanet(Long planetId, Long ownerId) {
        var activeJob = jobRepository.findByPlanetId(planetId);
        int shipyardLevel = buildingService.levelOf(planetId, SHIPYARD_KEY);

        return catalogService.ships().stream()
                .map(definition -> {
                    int owned = ownedQuantity(planetId, definition.key());
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getShipKey().equals(definition.key());
                    List<LockedRequirement> missing = missingRequirements(planetId, ownerId, definition.requirements());
                    return new ShipyardView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            owned,
                            definition.cargoCapacity(),
                            definition.hydrogenConsumption(),
                            definition.baseCost(),
                            unitBuildTimeSeconds(definition, shipyardLevel),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getQuantity() : null,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null,
                            missing.isEmpty(),
                            missing
                    );
                })
                .toList();
    }

    @Transactional
    public ShipyardBuildResponse queueShip(Long planetId, Long ownerId, String shipKey, int quantity) {
        if (jobRepository.findByPlanetId(planetId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A shipyard order is already in progress on this planet");
        }

        ShipDefinition definition = catalogService.ship(shipKey);
        if (!missingRequirements(planetId, ownerId, definition.requirements()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requirements not met for ship: " + shipKey);
        }

        ResourceCost cost = definition.baseCost().scaled(quantity);
        resourceService.debit(planetId, cost);

        int shipyardLevel = buildingService.levelOf(planetId, SHIPYARD_KEY);
        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plusSeconds(unitBuildTimeSeconds(definition, shipyardLevel) * quantity);
        jobRepository.save(new ShipyardJob(planetId, shipKey, quantity, startedAt, endsAt));

        return new ShipyardBuildResponse(shipKey, quantity, endsAt);
    }

    /**
     * Unlike {@code BuildingService}'s own (building-only) helper, ships can be gated by both
     * building level (checked on this specific planet - the shipyard is planet-local) and
     * technology level (checked account-wide) - mirrors {@code ResearchService.missingRequirements}'s
     * shape, which already handles both types.
     */
    private List<LockedRequirement> missingRequirements(Long planetId, Long ownerId, List<Requirement> requirements) {
        List<LockedRequirement> missing = new ArrayList<>();
        for (Requirement requirement : requirements) {
            int currentLevel = switch (requirement.type()) {
                case BUILDING -> buildingService.levelOf(planetId, requirement.key());
                case TECHNOLOGY -> researchService.levelOf(ownerId, requirement.key());
            };
            if (currentLevel < requirement.level()) {
                String label = requirement.type() == RequirementType.TECHNOLOGY
                        ? catalogService.technology(requirement.key()).name()
                        : catalogService.building(requirement.key()).name();
                missing.add(new LockedRequirement(label, requirement.level(), currentLevel));
            }
        }
        return missing;
    }

    /** Each shipyard level speeds up construction (level 0 - no shipyard yet - is baseline speed). */
    private static long unitBuildTimeSeconds(ShipDefinition definition, int shipyardLevel) {
        return Math.max(1, Math.round(definition.baseBuildTimeSeconds() / (1.0 + shipyardLevel)));
    }

    @Transactional
    public void completeDueJobs() {
        for (ShipyardJob job : jobRepository.findByEndsAtBefore(Instant.now())) {
            Ship ship = shipRepository.findByPlanetIdAndShipKey(job.getPlanetId(), job.getShipKey())
                    .orElseGet(() -> new Ship(job.getPlanetId(), job.getShipKey(), 0));
            ship.setQuantity(ship.getQuantity() + job.getQuantity());
            shipRepository.save(ship);
            jobRepository.delete(job);
        }
    }

    /** Dev-only convenience: sets every catalog ship's stationed quantity on a planet. */
    @Transactional
    public void stockAllShips(Long planetId, int quantity) {
        for (ShipDefinition definition : catalogService.ships()) {
            Ship ship = shipRepository.findByPlanetIdAndShipKey(planetId, definition.key())
                    .orElseGet(() -> new Ship(planetId, definition.key(), 0));
            ship.setQuantity(quantity);
            shipRepository.save(ship);
        }
    }

    /** Admin-triggered game reset: clears every stationed ship, shipyard order, and in-flight movement, game-wide. */
    @Transactional
    public void wipeAll() {
        movementRepository.deleteAll();
        jobRepository.deleteAll();
        shipRepository.deleteAll();
    }

    /** Wipes a single planet's stationed fleet and any pending shipyard order - used when a planet is destroyed. */
    @Transactional
    public void wipeAllShipsAndOrders(Long planetId) {
        shipRepository.deleteAll(shipRepository.findByPlanetId(planetId));
        jobRepository.findByPlanetId(planetId).ifPresent(jobRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<FleetMovementView> listMovements(Long ownerId) {
        return movementRepository.findByOwnerId(ownerId).stream()
                .map(fleetMovementMapper::toView)
                .toList();
    }

    /** Every in-flight movement (sent by anyone) currently headed for one of this player's own planets. */
    @Transactional(readOnly = true)
    public List<IncomingMovementView> listIncoming(Long ownerId) {
        Map<String, Planet> myPlanetsByCoordinates = planetService.listMine(ownerId).stream()
                .collect(Collectors.toMap(FleetService::coordinateKey, Function.identity()));
        if (myPlanetsByCoordinates.isEmpty()) {
            return List.of();
        }

        List<FleetMovement> incoming = movementRepository.findAll().stream()
                .filter(m -> myPlanetsByCoordinates.containsKey(coordinateKey(m.getTargetGalaxy(), m.getTargetSystem(), m.getTargetPosition())))
                .toList();

        Set<Long> senderIds = incoming.stream().map(FleetMovement::getOwnerId).collect(Collectors.toSet());
        Map<Long, String> usernamesBySenderId = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return incoming.stream()
                .map(m -> {
                    Planet target = myPlanetsByCoordinates.get(coordinateKey(m.getTargetGalaxy(), m.getTargetSystem(), m.getTargetPosition()));
                    return new IncomingMovementView(
                            m.getId(), fleetMovementMapper.shipsToList(m.getShips()), fleetMovementMapper.cargoToList(m.getCargo()),
                            m.getMissionType(), m.getOriginPlanetId(), usernamesBySenderId.getOrDefault(m.getOwnerId(), "unknown"),
                            target.getId(), target.getName(), m.getDepartedAt(), m.getArrivesAt()
                    );
                })
                .toList();
    }

    private static String coordinateKey(Planet planet) {
        return coordinateKey(planet.getGalaxy(), planet.getSystem(), planet.getPosition());
    }

    private static String coordinateKey(int galaxy, int system, int position) {
        return galaxy + ":" + system + ":" + position;
    }

    @Transactional
    public FleetMovementView dispatch(Long ownerId, DispatchRequest request) {
        Planet origin = planetService.getOwned(request.originPlanetId(), ownerId);

        if (origin.getGalaxy() == request.targetGalaxy() && origin.getSystem() == request.targetSystem()
                && origin.getPosition() == request.targetPosition()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must differ from the origin planet");
        }
        // Manifest-only checks first - cheap, no DB lookups, and fail fast before we even
        // bother resolving the target.
        validateNoDuplicateShipKeys(request.ships());
        validateSpecialShipComposition(request.ships());
        validateMissionTarget(ownerId, request);

        // Validate every ship type has enough stock before debiting any of them, so a fleet
        // with one under-stocked ship class fails cleanly instead of partially departing.
        Map<String, Ship> shipsByKey = new HashMap<>();
        for (ShipQuantity entry : request.ships()) {
            Ship ship = shipRepository.findByPlanetIdAndShipKey(origin.getId(), entry.shipKey())
                    .filter(s -> s.getQuantity() >= entry.quantity())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Not enough " + entry.shipKey() + " stationed on this planet"));
            shipsByKey.put(entry.shipKey(), ship);
        }

        int slowestSpeed = slowestShipSpeed(request.ships());
        DriveScope requiredScope = requiredScope(origin, request.targetGalaxy(), request.targetSystem());
        double driveMultiplier = researchService.speedMultiplierForDrive(ownerId, request.driveKey(), requiredScope)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chosen drive is not researched or is too wide-scoped for a " + requiredScope + " mission"));
        long distance = distanceBetween(origin, request.targetGalaxy(), request.targetSystem(), request.targetPosition());
        long travelSeconds = travelTimeSeconds(distance, slowestSpeed, driveMultiplier);

        // Fuel and (for TRANSPORT) cargo are debited before any ship quantity is mutated,
        // preserving the same "fails cleanly, nothing partially departs" guarantee as the
        // ship-stock check above.
        long fuelNeeded = fuelCost(request.ships(), distance, driveMultiplier);
        resourceService.debit(origin.getId(), ResourceKey.HYDROGEN, fuelNeeded);

        Map<ResourceKey, Long> cargoManifest = Map.of();
        if (request.missionType() == FleetMissionType.TRANSPORT) {
            cargoManifest = request.cargo().stream()
                    .collect(Collectors.toMap(ResourceQuantity::resourceKey, ResourceQuantity::amount));
            for (var entry : cargoManifest.entrySet()) {
                resourceService.debit(origin.getId(), entry.getKey(), entry.getValue());
            }
        }

        for (ShipQuantity entry : request.ships()) {
            Ship ship = shipsByKey.get(entry.shipKey());
            ship.setQuantity(ship.getQuantity() - entry.quantity());
            shipRepository.save(ship);
        }

        Instant departedAt = Instant.now();
        Instant arrivesAt = departedAt.plusSeconds(travelSeconds);
        Map<String, Integer> shipManifest = request.ships().stream()
                .collect(Collectors.toMap(ShipQuantity::shipKey, ShipQuantity::quantity));
        FleetMovement movement = movementRepository.save(new FleetMovement(
                origin.getId(), ownerId, shipManifest, cargoManifest, request.missionType(),
                request.targetGalaxy(), request.targetSystem(), request.targetPosition(), departedAt, arrivesAt
        ));

        return fleetMovementMapper.toView(movement);
    }

    private void validateNoDuplicateShipKeys(List<ShipQuantity> ships) {
        Set<String> seen = new HashSet<>();
        for (ShipQuantity entry : ships) {
            if (!seen.add(entry.shipKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate ship type in fleet: " + entry.shipKey());
            }
        }
    }

    /**
     * Manifest-wide composition rules for the two "weapon of last resort" ships - checked
     * regardless of mission type, since these are properties of the fleet itself, not of
     * where it's headed.
     */
    private void validateSpecialShipComposition(List<ShipQuantity> ships) {
        boolean hasBomb = ships.stream().anyMatch(s -> BOMB_KEY.equals(s.shipKey()) && s.quantity() > 0);
        boolean hasInvasionUnit = ships.stream().anyMatch(s -> INVASION_KEY.equals(s.shipKey()) && s.quantity() > 0);
        if (!hasBomb && !hasInvasionUnit) {
            return;
        }
        if (hasBomb && hasInvasionUnit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orbital Bombs and Invasion Units cannot be sent together");
        }
        if (ships.size() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orbital Bombs and Invasion Units cannot fly without an escort");
        }
        boolean hasGalaxyClassEscort = ships.stream().anyMatch(s -> GALAXY_CLASS_KEY.equals(s.shipKey()) && s.quantity() > 0);
        if (!hasGalaxyClassEscort) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orbital Bombs and Invasion Units require at least one Galaxy Class escort");
        }
    }

    /** A fleet travels at the speed of its slowest ship. */
    private int slowestShipSpeed(List<ShipQuantity> ships) {
        return ships.stream()
                .mapToInt(s -> catalogService.ship(s.shipKey()).speed())
                .min()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one ship is required"));
    }

    @Transactional
    public void completeDueMissions() {
        for (FleetMovement movement : movementRepository.findByArrivesAtBefore(Instant.now())) {
            switch (movement.getMissionType()) {
                case COLONIZE -> foundColony(movement);
                case STATION -> stationShips(movement);
                case ATTACK -> handleAttackArrival(movement);
                case ESPIONAGE -> resolveEspionage(movement);
                case TRANSPORT -> {
                    stationShips(movement);
                    deliverCargo(movement);
                }
                case BOMBARD -> handleBombardArrival(movement);
                case INVADE -> handleInvadeArrival(movement);
            }
            movementRepository.delete(movement);
        }
    }

    /**
     * Founds the colony (consuming the colony ship(s)) and stations every other ship class
     * that rode along in the same fleet directly on the new colony - lets a mixed fleet
     * colonize and garrison in a single dispatch.
     */
    private void foundColony(FleetMovement movement) {
        Planet colony = planetService.createColonyPlanetAt(
                movement.getOwnerId(),
                "Colony [%d:%d:%d]".formatted(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition()),
                movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition());
        movement.getShips().forEach((shipKey, quantity) -> {
            if (!COLONY_SHIP_KEY.equals(shipKey)) {
                creditShips(colony.getId(), shipKey, quantity);
            }
        });
        Map<String, Double> productionEfficiencies = buildingService.initializeProducingBuildings(colony.getId());
        events.publishEvent(new ColonyFounded(colony.getOwnerId(), colony.getId(), colony.getName(),
                colony.getResearchEfficiency(), productionEfficiencies));
    }

    /**
     * Every drive the player could pick for a hypothetical dispatch, each with the ETA it
     * would produce for the given fleet - lets the player compare before committing to one via
     * {@link #dispatch}, without actually sending anything. A mixed fleet travels at the speed
     * of its slowest ship.
     */
    public List<DriveOptionView> listDriveOptions(Long ownerId, DriveOptionsRequest request) {
        Planet origin = planetService.getOwned(request.originPlanetId(), ownerId);
        DriveScope requiredScope = requiredScope(origin, request.targetGalaxy(), request.targetSystem());
        int slowestSpeed = slowestShipSpeed(request.ships());
        long distance = distanceBetween(origin, request.targetGalaxy(), request.targetSystem(), request.targetPosition());

        return researchService.listAvailableDrives(ownerId, requiredScope).stream()
                .map(drive -> toDriveOptionView(drive, request.ships(), distance, slowestSpeed))
                .toList();
    }

    private DriveOptionView toDriveOptionView(DriveOption drive, List<ShipQuantity> ships, long distance, int shipSpeed) {
        long etaSeconds = travelTimeSeconds(distance, shipSpeed, drive.speedMultiplier());
        long fuelCost = fuelCost(ships, distance, drive.speedMultiplier());
        return new DriveOptionView(drive.key(), drive.name(), drive.driveScope(), drive.level(), drive.speedMultiplier(), etaSeconds, fuelCost);
    }

    private void validateMissionTarget(Long ownerId, DispatchRequest request) {
        switch (request.missionType()) {
            case COLONIZE -> {
                boolean hasColonyShip = request.ships().stream()
                        .anyMatch(s -> COLONY_SHIP_KEY.equals(s.shipKey()));
                if (!hasColonyShip) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A colonize mission needs at least one colony ship in the fleet");
                }
                if (!planetService.isColonizable(request.targetGalaxy(), request.targetSystem(), request.targetPosition())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Target position cannot be colonized");
                }
            }
            case STATION -> {
                Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
                requireNotDestroyed(target);
                if (!target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only station fleets at your own planets");
                }
            }
            case ATTACK -> {
                Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
                requireNotDestroyed(target);
                if (target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot attack your own planet");
                }
            }
            case ESPIONAGE -> {
                boolean hasProbe = request.ships().stream()
                        .anyMatch(s -> PROBE_SHIP_KEY.equals(s.shipKey()));
                if (!hasProbe) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An espionage mission needs at least one espionage probe in the fleet");
                }
                Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
                requireNotDestroyed(target);
                if (target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot spy on your own planet");
                }
            }
            case TRANSPORT -> {
                Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
                requireNotDestroyed(target);
                if (!target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only transport resources to your own planets");
                }
                for (ResourceQuantity cargo : request.cargo()) {
                    if (cargo.resourceKey() == ResourceKey.ENERGY || cargo.resourceKey() == ResourceKey.NONE) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transport " + cargo.resourceKey().getDisplayName());
                    }
                }
                long requestedCargo = request.cargo().stream().mapToLong(ResourceQuantity::amount).sum();
                long cargoCapacity = request.ships().stream()
                        .mapToLong(s -> (long) catalogService.ship(s.shipKey()).cargoCapacity() * s.quantity())
                        .sum();
                if (requestedCargo > cargoCapacity) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fleet cargo capacity exceeded");
                }
            }
            case BOMBARD -> validateSpecialMissionTarget(ownerId, request, BOMB_KEY, "An orbital bombardment needs at least one Orbital Bomb in the fleet");
            case INVADE -> validateSpecialMissionTarget(ownerId, request, INVASION_KEY, "An invasion needs at least one Invasion Unit in the fleet");
        }
    }

    /** Shared target validation for BOMBARD/INVADE: needs the special ship, a real enemy colony (never a homeworld, never already destroyed). */
    private void validateSpecialMissionTarget(Long ownerId, DispatchRequest request, String requiredShipKey, String missingShipMessage) {
        boolean hasRequiredShip = request.ships().stream().anyMatch(s -> requiredShipKey.equals(s.shipKey()));
        if (!hasRequiredShip) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingShipMessage);
        }
        Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
        requireNotDestroyed(target);
        if (target.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot target your own planet");
        }
        if (target.isHomeworld()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Homeworlds cannot be bombarded or invaded");
        }
    }

    private void requireNotDestroyed(Planet target) {
        if (target.isDestroyed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target planet has been destroyed");
        }
    }

    private void stationShips(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Station target planet no longer exists"));
        movement.getShips().forEach((shipKey, quantity) -> creditShips(target.getId(), shipKey, quantity));
    }

    /** Credits a TRANSPORT movement's cargo onto the target planet on arrival. */
    private void deliverCargo(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Transport target planet no longer exists"));
        movement.getCargo().forEach((key, amount) -> resourceService.credit(target.getId(), key, amount));
    }

    /** The espionage probe(s) always return to the origin planet, win or lose. */
    private void returnFleetToOrigin(FleetMovement movement) {
        movement.getShips().forEach((shipKey, quantity) -> creditShips(movement.getOriginPlanetId(), shipKey, quantity));
    }

    /**
     * Hands the attacking fleet off to {@code combat} for battle resolution - see
     * {@link AttackArrived}. Unlike the other mission types, ships are not credited back
     * here: {@code combat} decides how many survive and credits them back itself once
     * losses are known.
     */
    private void handleAttackArrival(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Attack target planet no longer exists"));
        events.publishEvent(new AttackArrived(movement.getOwnerId(), movement.getOriginPlanetId(),
                target.getOwnerId(), target.getId(), target.getName(), Map.copyOf(movement.getShips())));
    }

    /**
     * Hands the bombarding fleet off to {@code combat} - see {@link BombardArrived}. If the
     * target was already destroyed (e.g. by a different fleet that arrived first), the mission
     * is a no-op and the whole fleet simply returns home.
     */
    private void handleBombardArrival(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Bombard target planet no longer exists"));
        if (target.isDestroyed()) {
            returnFleetToOrigin(movement);
            return;
        }
        events.publishEvent(new BombardArrived(movement.getOwnerId(), movement.getOriginPlanetId(),
                target.getOwnerId(), target.getId(), target.getName(), Map.copyOf(movement.getShips())));
    }

    /**
     * Hands the invading fleet off to {@code combat} - see {@link InvadeArrived}. If the target
     * was already destroyed or is no longer enemy-owned (e.g. already invaded by someone else),
     * the mission is a no-op and the whole fleet simply returns home.
     */
    private void handleInvadeArrival(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Invade target planet no longer exists"));
        if (target.isDestroyed() || target.getOwnerId().equals(movement.getOwnerId())) {
            returnFleetToOrigin(movement);
            return;
        }
        events.publishEvent(new InvadeArrived(movement.getOwnerId(), movement.getOriginPlanetId(),
                target.getOwnerId(), target.getId(), target.getName(), Map.copyOf(movement.getShips())));
    }

    /**
     * Rolls the espionage attempt and publishes the outcome for {@code message} to notify the
     * attacker (and, on failure, the defender) — see {@link EspionageResolved}. The probe(s)
     * always return to the origin planet, win or lose; there's no combat resolution yet to
     * model a probe being destroyed.
     */
    private void resolveEspionage(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Espionage target planet no longer exists"));

        int espionageLevel = researchService.levelOf(movement.getOwnerId(), "espionage_technology");
        double chance = Math.min(ESPIONAGE_MAX_CHANCE, ESPIONAGE_BASE_CHANCE + ESPIONAGE_CHANCE_PER_LEVEL * espionageLevel);
        boolean success = ThreadLocalRandom.current().nextDouble() < chance;

        List<ShipQuantity> stationedShips = List.of();
        List<ResourceView> resources = List.of();
        if (success) {
            stationedShips = shipRepository.findByPlanetId(target.getId()).stream()
                    .map(ship -> new ShipQuantity(ship.getShipKey(), ship.getQuantity()))
                    .toList();
            resources = resourceService.raw(target.getId()).stream()
                    .map(resourceMapper::toView)
                    .toList();
        }

        events.publishEvent(new EspionageResolved(movement.getOwnerId(), target.getOwnerId(), target.getId(), target.getName(),
                success, stationedShips, resources));

        returnFleetToOrigin(movement);
    }

    /** Every ship stationed on a planet, keyed by ship catalog key. Used by combat to read defensive strength. */
    public Map<String, Integer> stationedShips(Long planetId) {
        return shipRepository.findByPlanetId(planetId).stream()
                .collect(Collectors.toMap(Ship::getShipKey, Ship::getQuantity));
    }

    /** Reduces stationed ship quantities by the given amounts. Used by combat to apply losses. */
    @Transactional
    public void applyLosses(Long planetId, Map<String, Integer> losses) {
        losses.forEach((shipKey, lost) -> {
            if (lost <= 0) {
                return;
            }
            shipRepository.findByPlanetIdAndShipKey(planetId, shipKey).ifPresent(ship -> {
                int remaining = Math.max(0, ship.getQuantity() - lost);
                if (remaining == 0) {
                    shipRepository.delete(ship);
                } else {
                    ship.setQuantity(remaining);
                    shipRepository.save(ship);
                }
            });
        });
    }

    /** Credits multiple ship types onto a planet at once - e.g. a surviving fleet returning from battle. */
    @Transactional
    public void creditShips(Long planetId, Map<String, Integer> ships) {
        ships.forEach((shipKey, quantity) -> {
            if (quantity > 0) {
                creditShips(planetId, shipKey, quantity);
            }
        });
    }

    private void creditShips(Long planetId, String shipKey, int quantity) {
        Ship ship = shipRepository.findByPlanetIdAndShipKey(planetId, shipKey)
                .orElseGet(() -> new Ship(planetId, shipKey, 0));
        ship.setQuantity(ship.getQuantity() + quantity);
        shipRepository.save(ship);
    }

    private DriveScope requiredScope(Planet origin, int targetGalaxy, int targetSystem) {
        if (origin.getGalaxy() != targetGalaxy) {
            return DriveScope.GALAXY;
        }
        if (origin.getSystem() != targetSystem) {
            return DriveScope.INTER_SYSTEM;
        }
        return DriveScope.SYSTEM;
    }

    private long distanceBetween(Planet origin, int targetGalaxy, int targetSystem, int targetPosition) {
        long distance = Math.abs(origin.getGalaxy() - targetGalaxy) * GALAXY_STEP
                + Math.abs(origin.getSystem() - targetSystem) * SYSTEM_STEP
                + Math.abs(origin.getPosition() - targetPosition) * POSITION_STEP;
        return Math.max(distance, POSITION_STEP);
    }

    private long travelTimeSeconds(long distance, int shipSpeed, double driveMultiplier) {
        double effectiveSpeed = shipSpeed * driveMultiplier;
        return Math.max(5, Math.round(distance / effectiveSpeed * 10));
    }

    /**
     * Total hydrogen a fleet burns for a one-way trip: each ship type's per-unit consumption
     * stat, summed across the fleet, scaled by distance (in {@link #SYSTEM_STEP}-sized "hops")
     * and made cheaper by a faster/more efficient drive - mirrors the same distance/drive
     * inputs {@link #travelTimeSeconds} already uses for ETA.
     */
    private long fuelCost(List<ShipQuantity> ships, long distance, double driveMultiplier) {
        long consumptionRate = ships.stream()
                .mapToLong(s -> (long) catalogService.ship(s.shipKey()).hydrogenConsumption() * s.quantity())
                .sum();
        if (consumptionRate <= 0) {
            return 0;
        }
        double hops = (double) distance / SYSTEM_STEP;
        return Math.max(1, Math.round(Math.ceil(consumptionRate * hops / driveMultiplier)));
    }

    private int ownedQuantity(Long planetId, String shipKey) {
        return shipRepository.findByPlanetIdAndShipKey(planetId, shipKey)
                .map(Ship::getQuantity)
                .orElse(0);
    }
}
