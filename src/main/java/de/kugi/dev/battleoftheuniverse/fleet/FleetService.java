package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionsRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.IncomingMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardBuildResponse;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardView;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.research.dto.DriveOption;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FleetService {

    /**
     * Rough distance units per galaxy/system/position step. Picked so a same-system
     * hop takes seconds and a cross-galaxy jump takes tens of seconds to a few minutes
     * at typical ship/drive speeds — tuned for a playable dev tempo, not realism.
     */
    private static final long GALAXY_STEP = 20_000;
    private static final long SYSTEM_STEP = 1_000;
    private static final long POSITION_STEP = 100;

    /** Only this ship can found a colony - matches the "colony_ship" entry in the ship catalog. */
    private static final String COLONY_SHIP_KEY = "colony_ship";

    private final ShipRepository shipRepository;
    private final ShipyardJobRepository jobRepository;
    private final FleetMovementRepository movementRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final ResearchService researchService;
    private final UserRepository userRepository;

    public FleetService(ShipRepository shipRepository, ShipyardJobRepository jobRepository,
                         FleetMovementRepository movementRepository, CatalogService catalogService,
                         ResourceService resourceService, PlanetService planetService,
                         ResearchService researchService, UserRepository userRepository) {
        this.shipRepository = shipRepository;
        this.jobRepository = jobRepository;
        this.movementRepository = movementRepository;
        this.catalogService = catalogService;
        this.resourceService = resourceService;
        this.planetService = planetService;
        this.researchService = researchService;
        this.userRepository = userRepository;
    }

    public List<ShipyardView> listForPlanet(Long planetId) {
        var activeJob = jobRepository.findByPlanetId(planetId);

        return catalogService.ships().stream()
                .map(definition -> {
                    int owned = ownedQuantity(planetId, definition.key());
                    boolean isBeingBuilt = activeJob.isPresent() && activeJob.get().getShipKey().equals(definition.key());
                    return new ShipyardView(
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            owned,
                            definition.baseCost(),
                            definition.baseBuildTimeSeconds(),
                            isBeingBuilt,
                            isBeingBuilt ? activeJob.get().getQuantity() : null,
                            isBeingBuilt ? activeJob.get().getEndsAt() : null
                    );
                })
                .toList();
    }

    @Transactional
    public ShipyardBuildResponse queueShip(Long planetId, String shipKey, int quantity) {
        if (jobRepository.findByPlanetId(planetId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A shipyard order is already in progress on this planet");
        }

        ShipDefinition definition = catalogService.ship(shipKey);

        ResourceCost cost = definition.baseCost().scaled(quantity);
        resourceService.debit(planetId, cost);

        Instant startedAt = Instant.now();
        Instant endsAt = startedAt.plusSeconds((long) definition.baseBuildTimeSeconds() * quantity);
        jobRepository.save(new ShipyardJob(planetId, shipKey, quantity, startedAt, endsAt));

        return new ShipyardBuildResponse(shipKey, quantity, endsAt);
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

    public List<FleetMovementView> listMovements(Long ownerId) {
        return movementRepository.findByOwnerId(ownerId).stream()
                .map(FleetService::toView)
                .toList();
    }

    private static FleetMovementView toView(FleetMovement m) {
        return new FleetMovementView(m.getId(), m.getOriginPlanetId(), toShipList(m.getShips()), m.getMissionType(),
                m.getTargetGalaxy(), m.getTargetSystem(), m.getTargetPosition(), m.getDepartedAt(), m.getArrivesAt());
    }

    private static List<ShipQuantity> toShipList(Map<String, Integer> ships) {
        return ships.entrySet().stream()
                .map(e -> new ShipQuantity(e.getKey(), e.getValue()))
                .toList();
    }

    /** Every in-flight movement (sent by anyone) currently headed for one of this player's own planets. */
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
                            m.getId(), toShipList(m.getShips()), m.getMissionType(),
                            m.getOriginPlanetId(), usernamesBySenderId.getOrDefault(m.getOwnerId(), "unknown"),
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
        validateMissionTarget(ownerId, request);
        validateNoDuplicateShipKeys(request.ships());

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
        for (ShipQuantity entry : request.ships()) {
            Ship ship = shipsByKey.get(entry.shipKey());
            ship.setQuantity(ship.getQuantity() - entry.quantity());
            shipRepository.save(ship);
        }

        int slowestSpeed = slowestShipSpeed(request.ships());
        long travelSeconds = travelSecondsForDrive(ownerId, origin, request.driveKey(), slowestSpeed,
                request.targetGalaxy(), request.targetSystem(), request.targetPosition());

        Instant departedAt = Instant.now();
        Instant arrivesAt = departedAt.plusSeconds(travelSeconds);
        Map<String, Integer> shipManifest = request.ships().stream()
                .collect(Collectors.toMap(ShipQuantity::shipKey, ShipQuantity::quantity));
        FleetMovement movement = movementRepository.save(new FleetMovement(
                origin.getId(), ownerId, shipManifest, request.missionType(),
                request.targetGalaxy(), request.targetSystem(), request.targetPosition(), departedAt, arrivesAt
        ));

        return toView(movement);
    }

    private void validateNoDuplicateShipKeys(List<ShipQuantity> ships) {
        Set<String> seen = new HashSet<>();
        for (ShipQuantity entry : ships) {
            if (!seen.add(entry.shipKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate ship type in fleet: " + entry.shipKey());
            }
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
                case ATTACK -> returnFleetToOrigin(movement);
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

        return researchService.listAvailableDrives(ownerId, requiredScope).stream()
                .map(drive -> toDriveOptionView(drive, origin, slowestSpeed, request.targetGalaxy(), request.targetSystem(), request.targetPosition()))
                .toList();
    }

    private DriveOptionView toDriveOptionView(DriveOption drive, Planet origin, int shipSpeed,
                                               int targetGalaxy, int targetSystem, int targetPosition) {
        long etaSeconds = travelTimeSeconds(origin, targetGalaxy, targetSystem, targetPosition,
                shipSpeed, drive.speedMultiplier());
        return new DriveOptionView(drive.key(), drive.name(), drive.driveScope(), drive.level(), drive.speedMultiplier(), etaSeconds);
    }

    private long travelSecondsForDrive(Long ownerId, Planet origin, String driveKey, int shipSpeed,
                                        int targetGalaxy, int targetSystem, int targetPosition) {
        DriveScope requiredScope = requiredScope(origin, targetGalaxy, targetSystem);
        double driveMultiplier = researchService.speedMultiplierForDrive(ownerId, driveKey, requiredScope)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chosen drive is not researched or not capable of a " + requiredScope + " mission"));

        return travelTimeSeconds(origin, targetGalaxy, targetSystem, targetPosition, shipSpeed, driveMultiplier);
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
                if (!target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only station fleets at your own planets");
                }
            }
            case ATTACK -> {
                Planet target = planetService.findAtPosition(request.targetGalaxy(), request.targetSystem(), request.targetPosition())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No planet at target coordinates"));
                if (target.getOwnerId().equals(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot attack your own planet");
                }
            }
        }
    }

    private void stationShips(FleetMovement movement) {
        Planet target = planetService.findAtPosition(movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition())
                .orElseThrow(() -> new IllegalStateException("Station target planet no longer exists"));
        movement.getShips().forEach((shipKey, quantity) -> creditShips(target.getId(), shipKey, quantity));
    }

    /** No combat resolution exists yet, so an attack fleet just turns around empty-handed. */
    private void returnFleetToOrigin(FleetMovement movement) {
        movement.getShips().forEach((shipKey, quantity) -> creditShips(movement.getOriginPlanetId(), shipKey, quantity));
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

    private long travelTimeSeconds(Planet origin, int targetGalaxy, int targetSystem, int targetPosition,
                                    int shipSpeed, double driveMultiplier) {
        long distance = Math.abs(origin.getGalaxy() - targetGalaxy) * GALAXY_STEP
                + Math.abs(origin.getSystem() - targetSystem) * SYSTEM_STEP
                + Math.abs(origin.getPosition() - targetPosition) * POSITION_STEP;
        distance = Math.max(distance, POSITION_STEP);

        double effectiveSpeed = shipSpeed * driveMultiplier;
        return Math.max(5, Math.round(distance / effectiveSpeed * 10));
    }

    private int ownedQuantity(Long planetId, String shipKey) {
        return shipRepository.findByPlanetIdAndShipKey(planetId, shipKey)
                .map(Ship::getQuantity)
                .orElse(0);
    }
}
