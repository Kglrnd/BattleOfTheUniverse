package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.fleet.dto.BuildShipsRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardBuildResponse;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardQueueView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardView;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planets/{planetId}/ships")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;
    private final PlanetService planetService;

    @GetMapping
    public List<ShipyardView> list(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return fleetService.listForPlanet(planetId, principal.getId());
    }

    @PostMapping("/{key}/build")
    public ShipyardBuildResponse build(@PathVariable Long planetId, @PathVariable String key,
                                        @Valid @RequestBody BuildShipsRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return fleetService.queueShip(planetId, principal.getId(), key, request.quantity());
    }

    @GetMapping("/queue")
    public ShipyardQueueView queue(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return fleetService.shipyardQueue(planetId);
    }
}
