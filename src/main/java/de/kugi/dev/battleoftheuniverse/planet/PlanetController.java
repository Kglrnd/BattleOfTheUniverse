package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.PlanetMapper;
import de.kugi.dev.battleoftheuniverse.planet.dto.PlanetView;
import de.kugi.dev.battleoftheuniverse.planet.dto.RenamePlanetRequest;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planets")
@RequiredArgsConstructor
public class PlanetController {

    private final PlanetService planetService;
    private final PlanetMapper planetMapper;

    @GetMapping
    public List<PlanetView> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return planetService.listMine(principal.getId()).stream().map(planetMapper::toView).toList();
    }

    @GetMapping("/home")
    public PlanetView home(@AuthenticationPrincipal AppUserPrincipal principal) {
        return planetMapper.toView(planetService.getHome(principal.getId()));
    }

    @GetMapping("/{id}")
    public PlanetView get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return planetMapper.toView(planetService.getOwned(id, principal.getId()));
    }

    @GetMapping("/by-owner/{ownerId}")
    public List<PlanetView> byOwner(@PathVariable Long ownerId) {
        return planetService.listMine(ownerId).stream().map(planetMapper::toView).toList();
    }

    @PatchMapping("/{id}/name")
    public PlanetView rename(@PathVariable Long id, @Valid @RequestBody RenamePlanetRequest request,
                              @AuthenticationPrincipal AppUserPrincipal principal) {
        return planetMapper.toView(planetService.rename(id, principal.getId(), request.name()));
    }
}
