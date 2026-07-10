package de.kugi.dev.battleoftheuniverse.highscore;

import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreResponseDto;
import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/highscore")
@RequiredArgsConstructor
public class HighscoreController {

    private final HighscoreService highscoreService;

    @GetMapping
    public HighscoreResponseDto get(@AuthenticationPrincipal AppUserPrincipal principal) {
        return highscoreService.get(principal.getId());
    }
}
