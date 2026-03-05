package com.nhavronskyi.progressmebackend.value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.value.dto.CreateValueRequest;
import com.nhavronskyi.progressmebackend.value.dto.UpdateValueRequest;
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
class UserValueControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;
    private String categoryId;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("val@example.com", "password123", "Val User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();

        // Create a category for values
        CreateCategoryRequest catReq = new CreateCategoryRequest("Health", null, "#27AE60", null);
        String catBody = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        categoryId = objectMapper.readTree(catBody).get("id").asText();
    }

    @Test
    void createValue() throws Exception {
        CreateValueRequest request = new CreateValueRequest("Discipline", "Staying consistent");

        mockMvc.perform(post("/api/categories/" + categoryId + "/values")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Discipline"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.sortOrder").value(0));
    }

    @Test
    void createValueForOtherUsersCategoryFails() throws Exception {
        // Register second user
        RegisterRequest register2 = new RegisterRequest("other@example.com", "password123", "Other");
        String body2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2)))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readValue(body2, AuthResponse.class).token();

        CreateValueRequest request = new CreateValueRequest("Hack", null);
        mockMvc.perform(post("/api/categories/" + categoryId + "/values")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateValue() throws Exception {
        String valueId = createVal("Discipline", null);
        UpdateValueRequest update = new UpdateValueRequest("Self-Discipline", "Updated description");

        mockMvc.perform(put("/api/values/" + valueId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Self-Discipline"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void deleteValue() throws Exception {
        String valueId = createVal("Discipline", null);

        mockMvc.perform(delete("/api/values/" + valueId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void listAllValues() throws Exception {
        createVal("Discipline", null);
        createVal("Consistency", null);

        mockMvc.perform(get("/api/values")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void categoryListIncludesNestedValues() throws Exception {
        createVal("Discipline", null);
        createVal("Consistency", null);

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].values.length()").value(2))
                .andExpect(jsonPath("$[0].values[0].name").value("Discipline"));
    }

    private String createVal(String name, String description) throws Exception {
        CreateValueRequest request = new CreateValueRequest(name, description);
        String body = mockMvc.perform(post("/api/categories/" + categoryId + "/values")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }
}
