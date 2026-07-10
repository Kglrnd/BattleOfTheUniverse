package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.planet.dto.AdminPlanetView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/planets")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
@RequiredArgsConstructor
public class AdminPlanetController {

    private final PlanetService planetService;

    @GetMapping
    public List<AdminPlanetView> list() {
        return planetService.listAllForAdmin();
    }
}
