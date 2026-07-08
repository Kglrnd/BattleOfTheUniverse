package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.SystemView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/systems")
public class SystemController {

    private final PlanetService planetService;

    public SystemController(PlanetService planetService) {
        this.planetService = planetService;
    }

    @GetMapping("/{galaxy}/{system}")
    public SystemView get(@PathVariable int galaxy, @PathVariable int system) {
        return planetService.getSystemView(galaxy, system);
    }
}
