package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.fleet.dto.BuildShipsRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardBuildResponse;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipyardView;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/planets/{planetId}/ships")
public class FleetController {

    private final FleetService fleetService;
    private final PlanetService planetService;

    public FleetController(FleetService fleetService, PlanetService planetService) {
        this.fleetService = fleetService;
        this.planetService = planetService;
    }

    @GetMapping
    public List<ShipyardView> list(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return fleetService.listForPlanet(planetId);
    }

    @PostMapping("/{key}/build")
    public ShipyardBuildResponse build(@PathVariable Long planetId, @PathVariable String key,
                                        @Valid @RequestBody BuildShipsRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return fleetService.queueShip(planetId, key, request.quantity());
    }

    private void requireOwnership(Long planetId, AppUserPrincipal principal) {
        if (!planetService.isOwnedBy(planetId, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found");
        }
    }
}
