package de.kugi.dev.battleoftheuniverse.config;

import de.kugi.dev.battleoftheuniverse.user.AppUserPrincipal;
import de.kugi.dev.battleoftheuniverse.user.UserMapper;
import de.kugi.dev.battleoftheuniverse.user.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserMapper userMapper;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * The h2-console servlet only actually exists when this is true (see {@code application-dev.yml}) - it's
     * never set in prod. Gating the permitAll/CSRF-exempt rules on the same flag (rather than hardcoding the
     * path as always-permitted) means a misconfigured deployment that silently falls back to the "dev" default
     * profile (e.g. a bare {@code java -jar} run that forgets to set {@code SPRING_PROFILES_ACTIVE=prod}) can't
     * end up with an unauthenticated SQL console reachable over the network.
     */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UrlBasedCorsConfigurationSource corsSource) {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        List<String> csrfIgnoredPaths = new ArrayList<>(List.of("/api/auth/register", "/api/auth/login"));
        if (h2ConsoleEnabled) {
            csrfIgnoredPaths.add("/h2-console/**");
        }

        http
                .cors(cors -> cors.configurationSource(corsSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers(csrfIgnoredPaths.toArray(String[]::new)))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(this::onAuthenticationRequired))
                .authorizeHttpRequests(auth -> {
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/registration-status").permitAll()
                            .requestMatchers("/api/version").permitAll()
                            .requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(this::onLoginSuccess)
                        .failureHandler(this::onLoginFailure))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value())));

        return http.build();
    }

    private void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                                 org.springframework.security.core.Authentication authentication) throws IOException {
        AppUserPrincipal principal = (AppUserPrincipal) Objects.requireNonNull(authentication.getPrincipal(),
                "Authenticated principal must not be null");
        UserView view = userMapper.toView(principal);
        writeJson(response, HttpStatus.OK, view);
    }

    private void onLoginFailure(HttpServletRequest request, HttpServletResponse response,
                                 org.springframework.security.core.AuthenticationException exception) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED, new ApiError(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password"));
    }

    /**
     * Without this, an unauthenticated request to a protected endpoint falls back to Spring
     * Security's default {@code formLogin} behavior: a 302 redirect to a login *page* that
     * doesn't exist on this API-only backend (the SPA's login route is client-side). A pure
     * JSON API needs a clean 401 instead - e.g. so the frontend can detect "session expired"
     * and redirect to /login itself, rather than the fetch/XHR call choking on an HTML response.
     */
    private void onAuthenticationRequired(HttpServletRequest request, HttpServletResponse response,
                                           org.springframework.security.core.AuthenticationException exception) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED, new ApiError(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
