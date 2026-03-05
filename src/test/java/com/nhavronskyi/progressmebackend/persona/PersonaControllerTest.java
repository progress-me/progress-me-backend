package com.nhavronskyi.progressmebackend.persona;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.persona.dto.UpdatePersonaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PersonaControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("persona@example.com", "password123", "Persona User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();

        token = objectMapper.readValue(body, AuthResponse.class).token();
    }

    @Test
    void getPersonaReturnsExisting() throws Exception {
        mockMvc.perform(get("/api/me/persona")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.displayName").isEmpty());
    }

    @Test
    void updatePersona() throws Exception {
        UpdatePersonaRequest request = new UpdatePersonaRequest("Nazar", 25, "A developer", "Coding", "Procrastination", "Tech, Music", "Wants to grow");

        mockMvc.perform(put("/api/me/persona")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nazar"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.bio").value("A developer"))
                .andExpect(jsonPath("$.strengths").value("Coding"))
                .andExpect(jsonPath("$.weaknesses").value("Procrastination"))
                .andExpect(jsonPath("$.interests").value("Tech, Music"))
                .andExpect(jsonPath("$.context").value("Wants to grow"));
    }

    @Test
    void updateThenGetPersists() throws Exception {
        UpdatePersonaRequest request = new UpdatePersonaRequest("Updated", 30, "Bio", null, null, null, null);

        mockMvc.perform(put("/api/me/persona")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/me/persona")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated"))
                .andExpect(jsonPath("$.age").value(30));
    }

    @Test
    void personaEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/me/persona"))
                .andExpect(status().isForbidden());
    }
}
