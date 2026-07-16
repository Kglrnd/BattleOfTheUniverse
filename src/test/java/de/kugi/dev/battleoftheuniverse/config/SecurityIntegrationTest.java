package de.kugi.dev.battleoftheuniverse.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the actual wired-together security/error-handling configuration
 * (SecurityConfig, CorsConfig, CsrfCookieFilter, GlobalExceptionHandler) end to end through
 * real HTTP requests - these classes are filter-chain/exception-mapping wiring, not
 * meaningfully testable by mocking their collaborators in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void publicEndpointsAreReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/version")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRejectUnauthenticatedRequestsWithAJsonApiErrorNotARedirect() throws Exception {
        // Without a custom AuthenticationEntryPoint, Spring Security's formLogin default is a
        // 302 to a login *page* - useless for a pure JSON API and confusing for the SPA (see
        // SecurityConfig.onAuthenticationRequired).
        mockMvc.perform(get("/api/planets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void registerThenLoginWithValidCredentialsSucceedsAndEstablishesASession() throws Exception {
        String username = "alice" + UUID.randomUUID().toString().substring(0, 8);
        registerUser(username, "alice123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", "alice123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        // The session cookie from login authenticates a subsequent request to a protected endpoint.
        mockMvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession)
                        loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void loginWithInvalidCredentialsReturnsAnApiErrorBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "nobody-" + UUID.randomUUID())
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void validationErrorsAreMappedToAnApiErrorWithFieldErrors() throws Exception {
        Map<String, String> blankUsername = Map.of(
                "username", "",
                "email", "not-an-email",
                "password", "short");

        mockMvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(blankUsername)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
    }

    @Test
    void accessDeniedIsMappedToA403ApiErrorForAnAuthenticatedNonAdmin() throws Exception {
        String username = "bob" + UUID.randomUUID().toString().substring(0, 8);
        registerUser(username, "bob12345");
        MvcResult loginResult = login(username, "bob12345");

        mockMvc.perform(get("/api/admin/users").session((org.springframework.mock.web.MockHttpSession)
                        loginResult.getRequest().getSession(false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void unknownRouteThrowsAResponseStatusExceptionMappedToAMatchingApiError() throws Exception {
        String username = "carol" + UUID.randomUUID().toString().substring(0, 8);
        registerUser(username, "carol1234");
        MvcResult loginResult = login(username, "carol1234");

        // Planet 9,999,999 doesn't exist - PlanetService.getOwned() throws a 404 ResponseStatusException.
        mockMvc.perform(get("/api/planets/9999999").session((org.springframework.mock.web.MockHttpSession)
                        loginResult.getRequest().getSession(false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Planet not found"));
    }

    private void registerUser(String username, String password) throws Exception {
        Map<String, String> request = Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", password);

        mockMvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
    }
}
