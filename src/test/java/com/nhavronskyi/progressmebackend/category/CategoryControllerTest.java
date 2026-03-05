package com.nhavronskyi.progressmebackend.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.category.dto.UpdateCategoryRequest;
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
class CategoryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("cat@example.com", "password123", "Cat User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();
    }

    @Test
    void createCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Health", "Physical and mental health", "#27AE60", null);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Health"))
                .andExpect(jsonPath("$.color").value("#27AE60"))
                .andExpect(jsonPath("$.sortOrder").value(0))
                .andExpect(jsonPath("$.values").isEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createCategoryDuplicateNameFails() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Health", null, null, null);
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategoryInvalidColorFails() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Health", null, "red", null);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCategories() throws Exception {
        createCat("Health", "#27AE60");
        createCat("Career", "#4A90D9");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sortOrder").value(0))
                .andExpect(jsonPath("$[1].sortOrder").value(1));
    }

    @Test
    void getCategory() throws Exception {
        String id = createCat("Health", "#27AE60");

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Health"))
                .andExpect(jsonPath("$.values").isEmpty());
    }

    @Test
    void updateCategory() throws Exception {
        String id = createCat("Health", "#27AE60");
        UpdateCategoryRequest update = new UpdateCategoryRequest("Wellness", null, "#E67E22", null);

        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wellness"))
                .andExpect(jsonPath("$.color").value("#E67E22"));
    }

    @Test
    void deleteCategory() throws Exception {
        String id = createCat("Health", "#27AE60");

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoryRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotSeeOtherUsersCategories() throws Exception {
        String catId = createCat("Health", "#27AE60");

        // Register second user
        RegisterRequest register2 = new RegisterRequest("other@example.com", "password123", "Other User");
        String body2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2)))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readValue(body2, AuthResponse.class).token();

        mockMvc.perform(get("/api/categories/" + catId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());
    }

    private String createCat(String name, String color) throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(name, null, color, null);
        String body = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }
}
