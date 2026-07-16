package de.kugi.dev.battleoftheuniverse.defense;

import de.kugi.dev.battleoftheuniverse.building.PlanetBuilding;
import de.kugi.dev.battleoftheuniverse.building.PlanetBuildingRepository;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
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
class DefenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlanetBuildingRepository planetBuildingRepository;
    @Autowired
    private ResourceService resourceService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void listsEveryCatalogTowerForYourOwnPlanetLockedWithoutADefenseFacility() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(get("/api/planets/" + homeworldId + "/defenses").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[?(@.key=='light_defense_tower')].unlocked").value(org.hamcrest.Matchers.contains(false)));
    }

    @Test
    void rejectsListingDefensesForAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId + "/defenses").session(otherSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void buildingATowerRequiresTheDefenseFacilityRequirement() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(post("/api/planets/" + homeworldId + "/defenses/light_defense_tower/build")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("quantity", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buildingATowerSucceedsOnceUnlockedAndAffordable() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        planetBuildingRepository.save(new PlanetBuilding(homeworldId, "defense_facility", 1));
        resourceService.credit(homeworldId, new ResourceCost(2000, 500, 0));

        mockMvc.perform(post("/api/planets/" + homeworldId + "/defenses/light_defense_tower/build")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("quantity", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.towerKey").value("light_defense_tower"))
                .andExpect(jsonPath("$.quantity").value(1));
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
