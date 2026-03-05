package com.nhavronskyi.progressmebackend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.ai.dto.UpdateAiSettingsRequest;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AiSettingsControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("ai-settings@example.com", "password123", "AI Settings User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();
    }

    @Test
    void getReturnsDefaults() throws Exception {
        mockMvc.perform(get("/api/me/ai-settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("balanced"))
                .andExpect(jsonPath("$.strictness").value(5))
                .andExpect(jsonPath("$.focusAreas").isArray())
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void updatePartialFields() throws Exception {
        UpdateAiSettingsRequest request = new UpdateAiSettingsRequest("strict", 8, null, null, null, null);

        mockMvc.perform(put("/api/me/ai-settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("strict"))
                .andExpect(jsonPath("$.strictness").value(8))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void updateAllFields() throws Exception {
        UpdateAiSettingsRequest request = new UpdateAiSettingsRequest(
                "gentle", 3,
                List.of("fitness", "sleep"),
                List.of("work"),
                "Be extra encouraging",
                "uk"
        );

        mockMvc.perform(put("/api/me/ai-settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("gentle"))
                .andExpect(jsonPath("$.strictness").value(3))
                .andExpect(jsonPath("$.focusAreas[0]").value("fitness"))
                .andExpect(jsonPath("$.focusAreas[1]").value("sleep"))
                .andExpect(jsonPath("$.ignoreAreas[0]").value("work"))
                .andExpect(jsonPath("$.customInstructions").value("Be extra encouraging"))
                .andExpect(jsonPath("$.language").value("uk"));
    }

    @Test
    void updateThenGetPersists() throws Exception {
        UpdateAiSettingsRequest request = new UpdateAiSettingsRequest("analytical", 9, null, null, null, null);

        mockMvc.perform(put("/api/me/ai-settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/me/ai-settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("analytical"))
                .andExpect(jsonPath("$.strictness").value(9));
    }

    @Test
    void requiresAuth() throws Exception {
        mockMvc.perform(get("/api/me/ai-settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidTone() throws Exception {
        UpdateAiSettingsRequest request = new UpdateAiSettingsRequest("invalid", null, null, null, null, null);

        mockMvc.perform(put("/api/me/ai-settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
