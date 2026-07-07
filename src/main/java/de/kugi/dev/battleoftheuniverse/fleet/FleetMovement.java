package de.kugi.dev.battleoftheuniverse.fleet;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    private String shipKey;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private FleetMissionType missionType;

    private int targetGalaxy;
    private int targetSystem;
    private int targetPosition;

    private Instant departedAt;

    private Instant arrivesAt;

    public FleetMovement(Long originPlanetId, Long ownerId, String shipKey, int quantity, FleetMissionType missionType,
                          int targetGalaxy, int targetSystem, int targetPosition, Instant departedAt, Instant arrivesAt) {
        this.originPlanetId = originPlanetId;
        this.ownerId = ownerId;
        this.shipKey = shipKey;
        this.quantity = quantity;
        this.missionType = missionType;
        this.targetGalaxy = targetGalaxy;
        this.targetSystem = targetSystem;
        this.targetPosition = targetPosition;
        this.departedAt = departedAt;
        this.arrivesAt = arrivesAt;
    }
}
