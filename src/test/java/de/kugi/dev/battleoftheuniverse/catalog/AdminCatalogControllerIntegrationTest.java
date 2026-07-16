package de.kugi.dev.battleoftheuniverse.catalog;

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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void nonAdminsCannotReadTheCatalog() throws Exception {
        String username = "player" + UUID.randomUUID().toString().substring(0, 8);
        registerPlayer(username);
        MockHttpSession session = login(username);

        mockMvc.perform(get("/api/admin/catalog/buildings").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsCanReadCatalogDataAndItsSchema() throws Exception {
        MockHttpSession session = bootstrapAdminAndLogin();

        mockMvc.perform(get("/api/admin/catalog/buildings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").isArray());

        mockMvc.perform(get("/api/admin/catalog/buildings/schema").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("object"));
    }

    @Test
    void unknownCatalogTypeReturns404() throws Exception {
        MockHttpSession session = bootstrapAdminAndLogin();

        mockMvc.perform(get("/api/admin/catalog/not-a-real-type").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unknown catalog type")));
    }

    @Test
    void adminsCanSaveValidCatalogDataAndItIsReflectedImmediately() throws Exception {
        MockHttpSession session = bootstrapAdminAndLogin();

        MvcResult current = mockMvc.perform(get("/api/admin/catalog/buildings").session(session))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(put("/api/admin/catalog/buildings")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(current.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").isArray());
    }

    @Test
    void savingInvalidCatalogDataIsRejected() throws Exception {
        MockHttpSession session = bootstrapAdminAndLogin();

        mockMvc.perform(put("/api/admin/catalog/buildings")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildings\": [{\"key\": \"broken\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid catalog data")));
    }

    private void registerPlayer(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "email", username + "@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated());
    }

    private MockHttpSession bootstrapAdminAndLogin() throws Exception {
        String username = "admin" + UUID.randomUUID().toString().substring(0, 8);
        User admin = new User(username, username + "@example.com", passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
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
