package com.nhavronskyi.progressmebackend.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.insight.dto.AnalyzeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InsightControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatClient chatClient;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("insight@example.com", "password123", "Insight User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();

        ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        AssistantMessage assistantMessage = new AssistantMessage("Great progress! Keep it up.");
        Generation generation = new Generation(assistantMessage);
        Usage usage = mock(Usage.class);
        when(usage.getTotalTokens()).thenReturn(150);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getUsage()).thenReturn(usage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation), metadata);

        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.options(any())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponse);
    }

    @Test
    void analyzeReturnsInsight() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("How am I doing?", "analysis");

        mockMvc.perform(post("/api/insights/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("analysis"))
                .andExpect(jsonPath("$.query").value("How am I doing?"))
                .andExpect(jsonPath("$.content").value("Great progress! Keep it up."))
                .andExpect(jsonPath("$.model").value("gemini-3-flash-preview"))
                .andExpect(jsonPath("$.tokensUsed").value(150));
    }

    @Test
    void analyzeDefaultsToAnalysisType() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("Check my progress", null);

        mockMvc.perform(post("/api/insights/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("analysis"));
    }

    @Test
    void listInsights() throws Exception {
        // Create an insight first
        AnalyzeRequest request = new AnalyzeRequest("Test query", "analysis");
        mockMvc.perform(post("/api/insights/analyze")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].query").value("Test query"));
    }

    @Test
    void getInsightById() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("Specific query", "pattern");
        String body = mockMvc.perform(post("/api/insights/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/insights/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Specific query"))
                .andExpect(jsonPath("$.type").value("pattern"));
    }

    @Test
    void getInsightNotFound() throws Exception {
        mockMvc.perform(get("/api/insights/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPrebuiltPrompts() throws Exception {
        mockMvc.perform(get("/api/insights/prompts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("WEEKLY_ANALYSIS"))
                .andExpect(jsonPath("$[0].label").value("Analyze my week"))
                .andExpect(jsonPath("$[0].query").isNotEmpty());
    }

    @Test
    void analyzeRequiresAuth() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("Test", null);
        mockMvc.perform(post("/api/insights/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyzeRejectsBlankQuery() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("", null);
        mockMvc.perform(post("/api/insights/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
