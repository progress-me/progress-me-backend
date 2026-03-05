package com.nhavronskyi.progressmebackend.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalStatusRequest;
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
class GoalControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;
    private String categoryId;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("goal@example.com", "password123", "Goal User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();

        CreateCategoryRequest catReq = new CreateCategoryRequest("Health", null, "#27AE60", null);
        String catBody = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        categoryId = objectMapper.readTree(catBody).get("id").asText();
    }

    @Test
    void createGoalWithCategories() throws Exception {
        String json = """
                {"title":"Run a marathon","description":"Complete a full marathon","categoryIds":["%s"],"startDate":"2026-03-01","targetDate":"2026-09-01"}
                """.formatted(categoryId);

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Run a marathon"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Health"))
                .andExpect(jsonPath("$.entryCount").value(0));
    }

    @Test
    void createGoalWithMultipleCategories() throws Exception {
        // Create a second category
        CreateCategoryRequest catReq = new CreateCategoryRequest("Career", null, "#4A90D9", null);
        String catBody = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        String categoryId2 = objectMapper.readTree(catBody).get("id").asText();

        String json = """
                {"title":"Work-life balance","categoryIds":["%s","%s"]}
                """.formatted(categoryId, categoryId2);

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categories.length()").value(2));
    }

    @Test
    void createGoalWithSmartTemplate() throws Exception {
        String json = """
                {"title":"Marathon","templateType":"smart","templateData":{"specific":"Run 42km marathon","measurable":"Complete within 4 hours","achievable":"Training 3x/week","relevant":"Health and discipline","time_bound":"By September 2026"}}
                """;

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateType").value("smart"))
                .andExpect(jsonPath("$.templateData.specific").value("Run 42km marathon"));
    }

    @Test
    void createGoalInvalidDateRangeFails() throws Exception {
        String json = """
                {"title":"Bad goal","startDate":"2026-09-01","targetDate":"2026-03-01"}
                """;

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listGoalsFilterByStatus() throws Exception {
        createGoalHelper("Goal 1");
        createGoalHelper("Goal 2");

        // 2 created + 1 auto-created default = 3 active goals
        mockMvc.perform(get("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getGoal() throws Exception {
        String id = createGoalHelper("My Goal");

        mockMvc.perform(get("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Goal"))
                .andExpect(jsonPath("$.entryCount").value(0));
    }

    @Test
    void updateGoal() throws Exception {
        String id = createGoalHelper("Original");
        String json = """
                {"title":"Updated Title"}
                """;

        mockMvc.perform(put("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void completeGoalSetsCompletedAt() throws Exception {
        String id = createGoalHelper("To Complete");
        UpdateGoalStatusRequest statusReq = new UpdateGoalStatusRequest("completed");

        mockMvc.perform(put("/api/goals/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void pauseAndResumeGoal() throws Exception {
        String id = createGoalHelper("Pausable");

        UpdateGoalStatusRequest pauseReq = new UpdateGoalStatusRequest("paused");
        mockMvc.perform(put("/api/goals/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pauseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        UpdateGoalStatusRequest resumeReq = new UpdateGoalStatusRequest("active");
        mockMvc.perform(put("/api/goals/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void deleteGoal() throws Exception {
        String id = createGoalHelper("To Delete");

        mockMvc.perform(delete("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String createGoalHelper(String title) throws Exception {
        String json = """
                {"title":"%s"}
                """.formatted(title);
        String body = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }
}
