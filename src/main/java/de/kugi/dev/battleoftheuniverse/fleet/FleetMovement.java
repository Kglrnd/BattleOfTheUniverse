package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "fleet_movements")
@Getter
@Setter
@NoArgsConstructor
public class FleetMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long originPlanetId;

    private Long ownerId;

    /** Ship key -> quantity. A fleet can mix multiple ship classes in one movement. */
    @ElementCollection
    @CollectionTable(name = "fleet_movement_ships", joinColumns = @JoinColumn(name = "movement_id"))
    @MapKeyColumn(name = "ship_key")
    @Column(name = "quantity")
    @Getter(AccessLevel.NONE)
    private Map<String, Integer> ships = new HashMap<>();

    /** Resource key -> amount. Only populated for TRANSPORT missions; empty otherwise. */
    @ElementCollection
    @CollectionTable(name = "fleet_movement_cargo", joinColumns = @JoinColumn(name = "movement_id"))
    @MapKeyColumn(name = "resource_key")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "amount")
    @Getter(AccessLevel.NONE)
    private Map<ResourceKey, Long> cargo = new HashMap<>();

    @Enumerated(EnumType.STRING)
    private FleetMissionType missionType;

    private int targetGalaxy;
    private int targetSystem;
    private int targetPosition;

    private Instant departedAt;

    private Instant arrivesAt;

    public FleetMovement(Long originPlanetId, Long ownerId, Map<String, Integer> ships, Map<ResourceKey, Long> cargo,
                          FleetMissionType missionType, int targetGalaxy, int targetSystem, int targetPosition,
                          Instant departedAt, Instant arrivesAt) {
        this.originPlanetId = originPlanetId;
        this.ownerId = ownerId;
        this.ships = new HashMap<>(ships);
        this.cargo = new HashMap<>(cargo);
        this.missionType = missionType;
        this.targetGalaxy = targetGalaxy;
        this.targetSystem = targetSystem;
        this.targetPosition = targetPosition;
        this.departedAt = departedAt;
        this.arrivesAt = arrivesAt;
    }

    /** Convenience overload for callers that never carry cargo (every mission but TRANSPORT). */
    public FleetMovement(Long originPlanetId, Long ownerId, Map<String, Integer> ships, FleetMissionType missionType,
                          int targetGalaxy, int targetSystem, int targetPosition, Instant departedAt, Instant arrivesAt) {
        this(originPlanetId, ownerId, ships, Map.of(), missionType, targetGalaxy, targetSystem, targetPosition, departedAt, arrivesAt);
    }

    /**
     * Always an unmodifiable view - callers (including async {@code @ApplicationModuleListener}s
     * that outlive this entity's persistence context) must never get a handle to the live,
     * Hibernate-managed map.
     */
    public Map<String, Integer> getShips() {
        return Collections.unmodifiableMap(ships);
    }

    /** See {@link #getShips()}. */
    public Map<ResourceKey, Long> getCargo() {
        return Collections.unmodifiableMap(cargo);
    }
}
