package de.kugi.dev.battleoftheuniverse.resource;

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
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void listsTheStarterResourceLedgerForYourOwnPlanetSortedByKey() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long homeworldId = homeworldId(session);

        mockMvc.perform(get("/api/planets/" + homeworldId + "/resources").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$[0].resourceKey").value("METAL"))
                .andExpect(jsonPath("$[0].amount").value(500));
    }

    @Test
    void rejectsAccessToAPlanetYouDoNotOwn() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        Long homeworldId = homeworldId(ownerSession);
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(get("/api/planets/" + homeworldId + "/resources").session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Planet not found"));
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
