package de.kugidev.battleoftheuniverse.config;

import de.kugidev.battleoftheuniverse.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {

        CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
            (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();

        switch (userPrincipal.getRole()) {
            case ADMIN:
                response.sendRedirect("/admin/dashboard");
                break;
            case PLAYER:
                response.sendRedirect("/player/dashboard");
                break;
            default:
                response.sendRedirect("/");
                break;
        }
    }
}
