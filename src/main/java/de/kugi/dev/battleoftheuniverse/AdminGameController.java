package de.kugi.dev.battleoftheuniverse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/game")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminGameController {

    private final GameResetService gameResetService;

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        gameResetService.resetGame();
    }
}
