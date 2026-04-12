package com.nhavronskyi.progressmebackend.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DashboardControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("dashboard@example.com", "password123", "Dashboard User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readValue(body, AuthResponse.class).token();
    }

    @Test
    void emptyDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.days").value(30))
                .andExpect(jsonPath("$.entryStats.total").value(0))
                .andExpect(jsonPath("$.entryStats.byType").isEmpty())
                .andExpect(jsonPath("$.entryStats.byDay").isEmpty())
                .andExpect(jsonPath("$.moodTrend.average").value(0.0))
                .andExpect(jsonPath("$.moodTrend.daily").isEmpty())
                .andExpect(jsonPath("$.energyTrend.average").value(0.0))
                .andExpect(jsonPath("$.goalSummary.active").value(0))
                .andExpect(jsonPath("$.goalSummary.completed").value(0))
                .andExpect(jsonPath("$.goalSummary.paused").value(0))
                .andExpect(jsonPath("$.goalSummary.items").isEmpty())
                .andExpect(jsonPath("$.streak.current").value(0))
                .andExpect(jsonPath("$.streak.longest").value(0))
                .andExpect(jsonPath("$.topCategories").isEmpty());
    }

    @Test
    void dashboardWithEntries() throws Exception {
        createEntry("journal", "Journal entry",  7,  6);
        createEntry("micro_note", "Quick note", null, null);
        createEntry("check_in", "Morning check-in",  5,  4);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryStats.total").value(3))
                .andExpect(jsonPath("$.entryStats.byType.journal").value(1))
                .andExpect(jsonPath("$.entryStats.byType.micro_note").value(1))
                .andExpect(jsonPath("$.entryStats.byType.check_in").value(1))
                .andExpect(jsonPath("$.entryStats.byDay").isNotEmpty());
    }

    @Test
    void dashboardMoodTrend() throws Exception {
        createEntry("journal", "Happy day",  8,  7);
        createEntry("journal", "Great day",  6,  5);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodTrend.average").value(7.0))
                .andExpect(jsonPath("$.moodTrend.daily").isNotEmpty())
                .andExpect(jsonPath("$.energyTrend.average").value(6.0))
                .andExpect(jsonPath("$.energyTrend.daily").isNotEmpty());
    }

    @Test
    void dashboardGoalSummary() throws Exception {
        String goal1Id = createGoalHelper("Active Goal");
        String goal2Id = createGoalHelper("Completed Goal");
        String goal3Id = createGoalHelper("Paused Goal");

        updateGoalStatus(goal2Id, "completed");
        updateGoalStatus(goal3Id, "paused");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalSummary.active").value(1))
                .andExpect(jsonPath("$.goalSummary.completed").value(1))
                .andExpect(jsonPath("$.goalSummary.paused").value(1))
                .andExpect(jsonPath("$.goalSummary.items.length()").value(3));
    }

    @Test
    void dashboardStreak() throws Exception {
        LocalDate today = LocalDate.now();
        // Create entries on 3 consecutive days: today, yesterday, day before
        createEntryWithDate("journal", "Today",  7,  6,
                today.atTime(10, 0).atOffset(ZoneOffset.UTC));
        createEntryWithDate("journal", "Yesterday",  6,  5,
                today.minusDays(1).atTime(10, 0).atOffset(ZoneOffset.UTC));
        createEntryWithDate("journal", "Day before",  5,  4,
                today.minusDays(2).atTime(10, 0).atOffset(ZoneOffset.UTC));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak.current").value(3))
                .andExpect(jsonPath("$.streak.longest").value(3));
    }

    private void createEntry(String type, String content, Integer mood, Integer energy) throws Exception {
        String moodStr = mood != null ? String.valueOf(mood) : "null";
        String energyStr = energy != null ? String.valueOf(energy) : "null";
        String json = """
                {"type":"%s","content":"%s","mood":%s,"energy":%s}
                """.formatted(type, content, moodStr, energyStr);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    private void createEntryWithDate(String type, String content, Integer mood, Integer energy,
                                     OffsetDateTime loggedAt) throws Exception {
        String dateStr = loggedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String json = """
                {"type":"%s","content":"%s","mood":%d,"energy":%d,"loggedAt":"%s"}
                """.formatted(type, content, mood, energy, dateStr);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
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

    private void updateGoalStatus(String goalId, String status) throws Exception {
        UpdateGoalStatusRequest statusReq = new UpdateGoalStatusRequest(status);
        mockMvc.perform(put("/api/goals/" + goalId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk());
    }
}
