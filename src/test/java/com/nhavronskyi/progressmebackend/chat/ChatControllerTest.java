package com.nhavronskyi.progressmebackend.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.ai.orchestrator.OrchestratorService;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.chat.dto.CreateSessionRequest;
import com.nhavronskyi.progressmebackend.chat.dto.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChatControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrchestratorService orchestratorService;

    @MockitoBean(name = "executionClient")
    private ChatClient executionClient;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("chat-test@example.com", "password123", "Chat User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();

        mockOrchestrator("AI response text");
        mockExecutionClient("AI response text");
    }

    @Test
    void getStartersReturnsPrebuiltPrompts() throws Exception {
        mockMvc.perform(get("/api/chat/starters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].label").exists())
                .andExpect(jsonPath("$[0].query").exists());
    }

    @Test
    void createSessionReturnsSession() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest("Analyze my week");

        mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createSessionRejectsBlankMessage() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest("");

        mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listSessionsReturnsPaginated() throws Exception {
        createTestSession("First session message");
        createTestSession("Second session message");

        mockMvc.perform(get("/api/chat/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void sendMessageToExistingSession() throws Exception {
        String sessionId = createTestSession("Hello");

        SendMessageRequest request = new SendMessageRequest("Follow up question");
        mockMvc.perform(post("/api/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.content").value("AI response text"));
    }

    @Test
    void getSessionHistoryReturnMessages() throws Exception {
        String sessionId = createTestSession("Hello");

        mockMvc.perform(get("/api/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].role").value("USER"))
                .andExpect(jsonPath("$.content[1].role").value("ASSISTANT"));
    }

    @Test
    void cannotAccessOtherUsersSession() throws Exception {
        String sessionId = createTestSession("My private session");

        RegisterRequest otherUser = new RegisterRequest("other@example.com", "password123", "Other User");
        String otherBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUser)))
                .andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readValue(otherBody, AuthResponse.class).token();

        mockMvc.perform(get("/api/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendMessageToNonexistentSessionReturns404() throws Exception {
        SendMessageRequest request = new SendMessageRequest("Hello");
        mockMvc.perform(post("/api/chat/sessions/00000000-0000-0000-0000-000000000000/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isForbidden());
    }

    private String createTestSession(String message) throws Exception {
        CreateSessionRequest request = new CreateSessionRequest(message);
        MvcResult result = mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void mockOrchestrator(String responseText) {
        when(orchestratorService.processMessage(any(), any(), anyList(), any()))
                .thenReturn(new TaskResult(responseText, null));
    }

    @SuppressWarnings("unchecked")
    private void mockExecutionClient(String responseText) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        Usage usage = mock(Usage.class);
        when(usage.getTotalTokens()).thenReturn(100);

        org.springframework.ai.chat.metadata.ChatResponseMetadata metadata =
                mock(org.springframework.ai.chat.metadata.ChatResponseMetadata.class);
        when(metadata.getUsage()).thenReturn(usage);

        Generation generation = new Generation(new AssistantMessage(responseText));
        ChatResponse chatResponse = new ChatResponse(java.util.List.of(generation), metadata);

        when(executionClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponse);
        when(callSpec.content()).thenReturn(responseText);
    }
}
