package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.catalog.DriveScope;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.catalog.ShipDefinition;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardBuildResponse;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardView;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

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

    private final ShipRepository shipRepository;
    private final ShipyardJobRepository jobRepository;
    private final FleetMovementRepository movementRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final ResearchService researchService;
    private final ApplicationEventPublisher events;

    public FleetService(ShipRepository shipRepository, ShipyardJobRepository jobRepository,
                         FleetMovementRepository movementRepository, CatalogService catalogService,
                         ResourceService resourceService, PlanetService planetService,
                         ResearchService researchService, ApplicationEventPublisher events) {
        this.shipRepository = shipRepository;
        this.jobRepository = jobRepository;
        this.movementRepository = movementRepository;
        this.catalogService = catalogService;
        this.resourceService = resourceService;
        this.planetService = planetService;
        this.researchService = researchService;
        this.events = events;
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

    public List<FleetMovementView> listMovements(Long ownerId) {
        return movementRepository.findByOwnerId(ownerId).stream()
                .map(m -> new FleetMovementView(
                        m.getId(), m.getOriginPlanetId(), m.getShipKey(), m.getQuantity(), m.getMissionType(),
                        m.getTargetGalaxy(), m.getTargetSystem(), m.getTargetPosition(), m.getDepartedAt(), m.getArrivesAt()
                ))
                .toList();
    }

    @Transactional
    public FleetMovementView dispatch(Long ownerId, DispatchRequest request) {
        Planet origin = planetService.getOwned(request.originPlanetId(), ownerId);

        if (origin.getGalaxy() == request.targetGalaxy() && origin.getSystem() == request.targetSystem()
                && origin.getPosition() == request.targetPosition()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must differ from the origin planet");
        }

        Ship ship = shipRepository.findByPlanetIdAndShipKey(origin.getId(), request.shipKey())
                .filter(s -> s.getQuantity() >= request.quantity())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Not enough ships stationed on this planet"));

        DriveScope requiredScope = requiredScope(origin, request.targetGalaxy(), request.targetSystem());
        double driveMultiplier = researchService.speedMultiplierFor(ownerId, requiredScope)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No researched drive is capable of a " + requiredScope + " mission"));

        ShipDefinition shipDefinition = catalogService.ship(request.shipKey());
        long travelSeconds = travelTimeSeconds(origin, request.targetGalaxy(), request.targetSystem(),
                request.targetPosition(), shipDefinition.speed(), driveMultiplier);

        ship.setQuantity(ship.getQuantity() - request.quantity());
        shipRepository.save(ship);

        Instant departedAt = Instant.now();
        Instant arrivesAt = departedAt.plusSeconds(travelSeconds);
        FleetMovement movement = movementRepository.save(new FleetMovement(
                origin.getId(), ownerId, request.shipKey(), request.quantity(), request.missionType(),
                request.targetGalaxy(), request.targetSystem(), request.targetPosition(), departedAt, arrivesAt
        ));

        return new FleetMovementView(movement.getId(), movement.getOriginPlanetId(), movement.getShipKey(),
                movement.getQuantity(), movement.getMissionType(), movement.getTargetGalaxy(), movement.getTargetSystem(),
                movement.getTargetPosition(), movement.getDepartedAt(), movement.getArrivesAt());
    }

    @Transactional
    public void completeDueMissions() {
        for (FleetMovement movement : movementRepository.findByArrivesAtBefore(Instant.now())) {
            if (movement.getMissionType() == FleetMissionType.COLONIZE) {
                events.publishEvent(new ColonizationArrived(
                        movement.getOwnerId(), movement.getShipKey(), movement.getQuantity(),
                        movement.getTargetGalaxy(), movement.getTargetSystem(), movement.getTargetPosition()
                ));
            }
            movementRepository.delete(movement);
        }
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
        long distance = (long) Math.abs(origin.getGalaxy() - targetGalaxy) * GALAXY_STEP
                + (long) Math.abs(origin.getSystem() - targetSystem) * SYSTEM_STEP
                + (long) Math.abs(origin.getPosition() - targetPosition) * POSITION_STEP;
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
