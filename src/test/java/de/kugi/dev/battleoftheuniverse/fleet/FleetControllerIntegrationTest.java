package de.kugi.dev.battleoftheuniverse.fleet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FleetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void listsEveryCatalogShipForYourOwnPlanetMostlyLocked() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(get("/api/planets/" + homeworldId + "/ships").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(12)))
                .andExpect(jsonPath("$[?(@.key=='light_fighter')].unlocked").value(org.hamcrest.Matchers.contains(true)));
    }

    @Test
    void rejectsListingShipsForAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId + "/ships").session(otherSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void buildingAShipRequiresTheUnlockRequirementsToBeMet() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        // heavy_fighter needs shipyard level 3, which a fresh homeworld doesn't have.
        mockMvc.perform(post("/api/planets/" + homeworldId + "/ships/heavy_fighter/build")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("quantity", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void movementsAndIncomingMovementsAreEmptyForAFreshAccount() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/fleet/movements").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        mockMvc.perform(get("/api/fleet/movements/incoming").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void driveOptionsRequiresAtLeastOneShipInTheManifest() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(post("/api/fleet/drive-options")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "originPlanetId", homeworldId,
                                "ships", java.util.List.of(),
                                "targetGalaxy", 1, "targetSystem", 1, "targetPosition", 1))))
                .andExpect(status().isBadRequest());
    }

    private Long homeworldId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/planets/home").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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
