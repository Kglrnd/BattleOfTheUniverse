package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login/logout themselves are handled declaratively by Spring Security's form-login and
 * logout filters (see {@code SecurityConfig}), which intercept
 * {@code /api/auth/login} and {@code /api/auth/logout} before they ever reach a
 * controller. This class only covers registration and the "who am I" check.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userMapper.toView(principal);
    }
}
