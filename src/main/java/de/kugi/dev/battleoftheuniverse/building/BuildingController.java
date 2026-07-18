package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.building.dto.BuildingView;
import de.kugi.dev.battleoftheuniverse.building.dto.ResourceProductionView;
import de.kugi.dev.battleoftheuniverse.building.dto.UpgradeResponse;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planets/{planetId}/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;
    private final PlanetService planetService;

    @GetMapping
    public List<BuildingView> list(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return buildingService.listForPlanet(planetId);
    }

    @PostMapping("/{key}/upgrade")
    public UpgradeResponse upgrade(@PathVariable Long planetId, @PathVariable String key,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return buildingService.upgrade(planetId, key);
    }

    @GetMapping("/production")
    public List<ResourceProductionView> production(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        planetService.requireOwned(planetId, principal.getId());
        return buildingService.productionOverview(planetId);
    }
}
