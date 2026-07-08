package de.kugi.dev.battleoftheuniverse.fleet;

import de.kugi.dev.battleoftheuniverse.fleet.dto.DispatchRequest;
import de.kugi.dev.battleoftheuniverse.fleet.dto.DriveOptionView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.FleetMovementView;
import de.kugi.dev.battleoftheuniverse.fleet.dto.IncomingMovementView;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fleet")
public class FleetMissionController {

    private final FleetService fleetService;

    public FleetMissionController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @PostMapping("/dispatch")
    public FleetMovementView dispatch(@Valid @RequestBody DispatchRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return fleetService.dispatch(principal.getId(), request);
    }

    @GetMapping("/movements")
    public List<FleetMovementView> movements(@AuthenticationPrincipal AppUserPrincipal principal) {
        return fleetService.listMovements(principal.getId());
    }

    @GetMapping("/movements/incoming")
    public List<IncomingMovementView> incomingMovements(@AuthenticationPrincipal AppUserPrincipal principal) {
        return fleetService.listIncoming(principal.getId());
    }

    @GetMapping("/drive-options")
    public List<DriveOptionView> driveOptions(@RequestParam Long originPlanetId, @RequestParam String shipKey,
                                               @RequestParam int targetGalaxy, @RequestParam int targetSystem,
                                               @RequestParam int targetPosition,
                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        return fleetService.listDriveOptions(principal.getId(), originPlanetId, shipKey,
                targetGalaxy, targetSystem, targetPosition);
    }
}
