package de.kugi.dev.battleoftheuniverse.research;

import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.dto.ResearchStartResponse;
import de.kugi.dev.battleoftheuniverse.research.dto.StartResearchRequest;
import de.kugi.dev.battleoftheuniverse.research.dto.TechnologyView;
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
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;
    private final PlanetService planetService;

    @GetMapping
    public List<TechnologyView> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return researchService.listForUser(principal.getId());
    }

    @PostMapping("/{key}/start")
    public ResearchStartResponse start(@PathVariable String key, @Valid @RequestBody StartResearchRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        if (!planetService.isOwnedBy(request.planetId(), principal.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planet not found");
        }
        return researchService.startResearch(principal.getId(), request.planetId(), key);
    }
}
