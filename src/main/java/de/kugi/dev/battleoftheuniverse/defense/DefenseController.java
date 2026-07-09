package de.kugi.dev.battleoftheuniverse.defense;

import de.kugi.dev.battleoftheuniverse.defense.dto.BuildTowerRequest;
import de.kugi.dev.battleoftheuniverse.defense.dto.TowerBuildResponse;
import de.kugi.dev.battleoftheuniverse.defense.dto.TowerView;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/planets/{planetId}/defenses")
@RequiredArgsConstructor
public class DefenseController {

    private final DefenseService defenseService;
    private final PlanetService planetService;

    @GetMapping
    public List<TowerView> list(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return defenseService.listForPlanet(planetId);
    }

    @PostMapping("/{key}/build")
    public TowerBuildResponse build(@PathVariable Long planetId, @PathVariable String key,
                                     @Valid @RequestBody BuildTowerRequest request,
                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return defenseService.queueTower(planetId, key, request.quantity());
    }

    private void requireOwnership(Long planetId, AppUserPrincipal principal) {
        if (!planetService.isOwnedBy(planetId, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found");
        }
    }
}
