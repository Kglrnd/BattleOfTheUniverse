package de.kugi.dev.battleoftheuniverse.resource;

import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceMapper;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceView;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/planets/{planetId}/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final PlanetService planetService;
    private final ResourceMapper resourceMapper;

    @GetMapping
    public List<ResourceView> resources(@PathVariable Long planetId, @AuthenticationPrincipal AppUserPrincipal principal) {
        requireOwnership(planetId, principal);
        return resourceService.raw(planetId).stream().map(resourceMapper::toView).toList();
    }

    private void requireOwnership(Long planetId, AppUserPrincipal principal) {
        if (!planetService.isOwnedBy(planetId, principal.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found");
        }
    }
}
