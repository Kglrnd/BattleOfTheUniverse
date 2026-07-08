package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.PlanetView;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planets")
public class PlanetController {

    private final PlanetService planetService;

    public PlanetController(PlanetService planetService) {
        this.planetService = planetService;
    }

    @GetMapping
    public List<PlanetView> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return planetService.listMine(principal.getId()).stream().map(PlanetView::from).toList();
    }

    @GetMapping("/home")
    public PlanetView home(@AuthenticationPrincipal AppUserPrincipal principal) {
        return PlanetView.from(planetService.getHome(principal.getId()));
    }

    @GetMapping("/{id}")
    public PlanetView get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return PlanetView.from(planetService.getOwned(id, principal.getId()));
    }
}
