package de.kugi.dev.battleoftheuniverse.building;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
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
class BuildingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ResourceService resourceService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void listsEveryCatalogBuildingForYourOwnPlanet() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(get("/api/planets/" + homeworldId + "/buildings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='main_building')].level").value(org.hamcrest.Matchers.contains(1)))
                .andExpect(jsonPath("$[?(@.key=='metal_mine')].level").value(org.hamcrest.Matchers.contains(0)));
    }

    @Test
    void rejectsListingBuildingsForAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId + "/buildings").session(otherSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void productionOverviewShowsBaseProductionAtLevelZeroWindowedZeroToFive() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(get("/api/planets/" + homeworldId + "/buildings/production").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.buildingKey=='metal_mine')].currentLevel").value(org.hamcrest.Matchers.contains(0)))
                .andExpect(jsonPath("$[?(@.buildingKey=='metal_mine')].currentProductionPerHour")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.greaterThan(0.0))))
                .andExpect(jsonPath("$[?(@.buildingKey=='metal_mine')].levels[0].level").value(org.hamcrest.Matchers.contains(0)))
                .andExpect(jsonPath("$[?(@.buildingKey=='metal_mine')].levels[5].level").value(org.hamcrest.Matchers.contains(5)))
                .andExpect(jsonPath("$[?(@.buildingKey=='research_lab')]").value(org.hamcrest.Matchers.empty()));
    }

    @Test
    void rejectsProductionOverviewForAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId + "/buildings/production").session(otherSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void upgradingAnAffordableBuildingQueuesConstruction() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(post("/api/planets/" + homeworldId + "/buildings/metal_mine/upgrade")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingKey").value("metal_mine"))
                .andExpect(jsonPath("$.targetLevel").value(1));
    }

    @Test
    void aSecondUpgradeWhileOneIsInProgressIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        mockMvc.perform(post("/api/planets/" + homeworldId + "/buildings/metal_mine/upgrade")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/planets/" + homeworldId + "/buildings/crystal_mine/upgrade")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in progress")));
    }

    @Test
    void upgradingAnUnaffordableBuildingIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        // Drain the starter ledger so even the cheap metal_mine level 1 can't be afforded.
        resourceService.debit(homeworldId, ResourceKey.METAL, 500);
        resourceService.debit(homeworldId, ResourceKey.CRYSTAL, 300);

        mockMvc.perform(post("/api/planets/" + homeworldId + "/buildings/metal_mine/upgrade")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Not enough resources"));
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
