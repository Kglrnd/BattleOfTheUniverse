package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.SystemView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/systems")
@RequiredArgsConstructor
public class SystemController {

    private final PlanetService planetService;

    @GetMapping("/{galaxy}/{system}")
    public SystemView get(@PathVariable int galaxy, @PathVariable int system) {
        return planetService.getSystemView(galaxy, system);
    }
}
