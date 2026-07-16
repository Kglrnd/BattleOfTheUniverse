package de.kugi.dev.battleoftheuniverse.planet;

import de.kugi.dev.battleoftheuniverse.user.Role;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mineListsOnlyYourOwnPlanets() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/planets").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].homeworld").value(true));
    }

    @Test
    void getReturnsAnOwnedPlanetByIdAnd404sForOneYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(homeworldId));

        mockMvc.perform(get("/api/planets/" + homeworldId).session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Planet not found"));
    }

    @Test
    void byOwnerListsAnyUsersPlanetsUnscoped() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        Long ownerId = jsonMapper.readTree(mockMvc.perform(get("/api/auth/me").session(session))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/planets/by-owner/" + ownerId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(homeworldId));
    }

    @Test
    void renameUpdatesYourOwnPlanetsName() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(patch("/api/planets/" + homeworldId + "/name")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("name", "  New Home  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Home"));
    }

    @Test
    void renameRejectsAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(patch("/api/planets/" + homeworldId + "/name")
                        .session(otherSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("name", "Hijacked"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameRejectsABlankName() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(patch("/api/planets/" + homeworldId + "/name")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("name", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void systemViewReturnsEveryUsableSlot() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/systems/1/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.galaxy").value(1))
                .andExpect(jsonPath("$.system").value(1))
                .andExpect(jsonPath("$.slots").isArray());
    }

    @Test
    void systemViewRejectsOutOfBoundsCoordinates() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/systems/0/1").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminsCannotListAllPlanets() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/admin/planets").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsCanListEveryPlanetWithOwnerUsernames() throws Exception {
        MockHttpSession playerSession = registerAndLogin();
        Long homeworldId = homeworldId(playerSession);
        MockHttpSession adminSession = bootstrapAdminAndLogin();

        mockMvc.perform(get("/api/admin/planets").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + homeworldId + ")].ownerUsername", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    private Long homeworldId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/planets/home").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private MockHttpSession bootstrapAdminAndLogin() throws Exception {
        String username = "admin" + UUID.randomUUID().toString().substring(0, 8);
        User admin = new User(username, username + "@example.com", passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return login(username);
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "player" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated());
        return login(username);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
