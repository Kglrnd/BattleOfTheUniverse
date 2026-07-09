package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/planets/{planetId}/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;
    private final PlanetService planetService;

    @GetMapping
    public List<BuildingView> list(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return buildingService.listForPlanet(planetId);
    }

    @PostMapping("/{key}/upgrade")
    public UpgradeResponse upgrade(@PathVariable Long planetId, @PathVariable String key,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return buildingService.upgrade(planetId, key);
    }

    private void requireOwnership(Long planetId, AppUserPrincipal principal) {
        if (!planetService.isOwnedBy(planetId, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found");
        }
    }
}
