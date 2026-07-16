package de.kugi.dev.battleoftheuniverse.research;

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
class ResearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlanetBuildingRepository planetBuildingRepository;
    @Autowired
    private ResourceService resourceService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void listsEveryCatalogTechnologyForAFreshAccountAllLocked() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/research").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    @Test
    void listsTheHomeworldAsAResearchPlanetOptionWithNoResearchLabYet() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/research/planets").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].researchLabLevel").value(0))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void activatingAPlanetWithoutAResearchLabIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(post("/api/research/planets/" + homeworldId + "/activate")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Research Lab")));
    }

    @Test
    void activatingAPlanetWithAResearchLabSucceedsAndUnlocksStartingResearch() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        planetBuildingRepository.save(new PlanetBuilding(homeworldId, "research_lab", 1));
        resourceService.credit(homeworldId, new ResourceCost(0, 1000, 500));

        mockMvc.perform(post("/api/research/planets/" + homeworldId + "/activate")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.researchLabLevel").value(1));

        mockMvc.perform(post("/api/research/energy_technology/start")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technologyKey").value("energy_technology"))
                .andExpect(jsonPath("$.targetLevel").value(1));

        // A second research can't start while one is already in progress.
        mockMvc.perform(post("/api/research/energy_technology/start")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in progress")));
    }

    @Test
    void activatingAPlanetYouDoNotOwnIsRejected() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(post("/api/research/planets/" + homeworldId + "/activate")
                        .session(otherSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void startingResearchWithoutAnActiveResearchPlanetIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(post("/api/research/energy_technology/start")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No active research planet selected"));
    }

    @Test
    void startingResearchWithUnmetTechnologyPrerequisitesIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);
        planetBuildingRepository.save(new PlanetBuilding(homeworldId, "research_lab", 1));
        mockMvc.perform(post("/api/research/planets/" + homeworldId + "/activate")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        // laser_technology requires energy_technology level 1, which hasn't been researched yet.
        mockMvc.perform(post("/api/research/laser_technology/start")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Requirements not met")));
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
