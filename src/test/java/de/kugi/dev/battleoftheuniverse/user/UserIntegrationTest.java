package de.kugi.dev.battleoftheuniverse.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * There's no self-service way to become an admin (that's the point), so admin-flow tests
 * bootstrap one directly via the repository/password encoder rather than through the API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void meReturnsTheAuthenticatedUsersProfile() throws Exception {
        String username = register();
        MockHttpSession session = login(username, "password123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void updateLanguagePatchesThePreferredLanguage() throws Exception {
        String username = register();
        MockHttpSession session = login(username, "password123");

        mockMvc.perform(patch("/api/auth/me/language")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("language", "de"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("de"));
    }

    @Test
    void nonAdminsCannotListUsers() throws Exception {
        String username = register();
        MockHttpSession session = login(username, "password123");

        mockMvc.perform(get("/api/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsCanListAndChangeRolesButNotTheirOwn() throws Exception {
        Admin admin = bootstrapAdmin();
        String playerUsername = register();

        mockMvc.perform(get("/api/admin/users").session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='" + playerUsername + "')]").exists());

        Long playerId = userRepository.findByUsername(playerUsername).orElseThrow().getId();
        MockHttpSession adminSession = admin.session();
        Long adminId = admin.userId();

        mockMvc.perform(patch("/api/admin/users/" + playerId + "/role")
                        .session(adminSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("role", "MODERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));

        mockMvc.perform(patch("/api/admin/users/" + adminId + "/role")
                        .session(adminSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("role", "PLAYER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot change your own role"));
    }

    @Test
    void adminCanTriggerAGameReset() throws Exception {
        Admin admin = bootstrapAdmin();

        mockMvc.perform(post("/api/admin/game/reset")
                        .session(admin.session())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonAdminsCannotTriggerAGameReset() throws Exception {
        String username = register();
        MockHttpSession session = login(username, "password123");

        mockMvc.perform(post("/api/admin/game/reset")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    private record Admin(Long userId, MockHttpSession session) {
    }

    private Admin bootstrapAdmin() throws Exception {
        String username = "admin" + UUID.randomUUID().toString().substring(0, 8);
        User admin = new User(username, username + "@example.com", passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);
        return new Admin(admin.getId(), login(username, "password123"));
    }

    private String register() throws Exception {
        String username = "player" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated());
        return username;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
