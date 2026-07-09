package de.kugi.dev.battleoftheuniverse.fleet.dto;

import de.kugi.dev.battleoftheuniverse.fleet.FleetMovement;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface FleetMovementMapper {

    FleetMovementView toView(FleetMovement movement);

    default List<ShipQuantity> shipsToList(Map<String, Integer> ships) {
        return ships.entrySet().stream()
                .map(e -> new ShipQuantity(e.getKey(), e.getValue()))
                .toList();
    }
}
