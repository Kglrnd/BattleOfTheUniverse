package de.kugi.dev.battleoftheuniverse.message;

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
class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void sendingReadingAndMarkingAMessageReadWorksEndToEnd() throws Exception {
        String sender = register("sender");
        String recipient = register("recipient");
        MockHttpSession senderSession = login(sender);
        MockHttpSession recipientSession = login(recipient);

        MvcResult sendResult = mockMvc.perform(post("/api/messages")
                        .session(senderSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "recipientUsername", recipient,
                                "subject", "Hello",
                                "body", "Hi there"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Hello"))
                .andReturn();

        // Sender's sent folder shows it.
        mockMvc.perform(get("/api/messages/sent").session(senderSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Hello"));

        // Recipient's inbox shows it, unread.
        mockMvc.perform(get("/api/messages/unread-count").session(recipientSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        Long messageId = extractId(sendResult);
        mockMvc.perform(post("/api/messages/" + messageId + "/read")
                        .session(recipientSession)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt", org.hamcrest.Matchers.notNullValue()));

        mockMvc.perform(get("/api/messages/unread-count").session(recipientSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(get("/api/messages/inbox").session(recipientSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Hello"))
                .andExpect(jsonPath("$[0].senderUsername").value(sender));
    }

    @Test
    void sendingToYourselfIsRejected() throws Exception {
        String username = register("solo");
        MockHttpSession session = login(username);

        mockMvc.perform(post("/api/messages")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "recipientUsername", username,
                                "subject", "Hi",
                                "body", "Hello"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot send a message to yourself"));
    }

    private Long extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        var node = jsonMapper.readTree(body);
        return node.get("id").asLong();
    }

    private String register(String prefix) throws Exception {
        String username = prefix + UUID.randomUUID().toString().substring(0, 8);
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
