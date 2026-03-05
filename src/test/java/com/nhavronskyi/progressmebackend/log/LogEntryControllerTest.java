package com.nhavronskyi.progressmebackend.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.log.dto.CreateLogEntryRequest;
import com.nhavronskyi.progressmebackend.log.dto.UpdateLogEntryRequest;
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
class LogEntryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("log@example.com", "password123", "Log User");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn().getResponse().getContentAsString();

        token = objectMapper.readValue(body, AuthResponse.class).token();
    }

    @Test
    void createLogEntry() throws Exception {
        CreateLogEntryRequest request = new CreateLogEntryRequest("journal", "Had a great day",  8,  7, null, null, null);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("journal"))
                .andExpect(jsonPath("$.content").value("Had a great day"))
                .andExpect(jsonPath("$.mood").value(8))
                .andExpect(jsonPath("$.energy").value(7))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createLogEntryWithInvalidTypeFails() throws Exception {
        CreateLogEntryRequest request = new CreateLogEntryRequest("invalid_type", "content",  5,  5, null, null, null);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLogEntryWithoutAuthFails() throws Exception {
        CreateLogEntryRequest request = new CreateLogEntryRequest("journal", "content", null, null, null, null, null);

        mockMvc.perform(post("/api/log-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listLogEntries() throws Exception {
        createEntry("journal", "Entry 1",  5,  5);
        createEntry("micro_note", "Entry 2", null, null);

        mockMvc.perform(get("/api/log-entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void listLogEntriesFilterByType() throws Exception {
        createEntry("journal", "Journal entry",  5,  5);
        createEntry("micro_note", "Micro note", null, null);

        mockMvc.perform(get("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "journal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("journal"));
    }

    @Test
    void getLogEntry() throws Exception {
        String id = createEntry("journal", "My entry",  7,  6);

        mockMvc.perform(get("/api/log-entries/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("My entry"))
                .andExpect(jsonPath("$.mood").value(7));
    }

    @Test
    void getNonExistentEntryFails() throws Exception {
        mockMvc.perform(get("/api/log-entries/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateLogEntry() throws Exception {
        String id = createEntry("journal", "Original",  5,  5);
        UpdateLogEntryRequest updateRequest = new UpdateLogEntryRequest("Updated content",  9, null, null, null, null);

        mockMvc.perform(put("/api/log-entries/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.mood").value(9))
                .andExpect(jsonPath("$.energy").value(5));
    }

    @Test
    void deleteLogEntry() throws Exception {
        String id = createEntry("journal", "To delete",  5,  5);

        mockMvc.perform(delete("/api/log-entries/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/log-entries/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEntryWithTags() throws Exception {
        // Create category + value
        String catId = createCategoryHelper("Focus");
        String valueId = createValueHelper(catId, "Discipline");

        String json = """
                {"type":"journal","content":"Tagged entry","tagValueIds":["%s"]}
                """.formatted(valueId);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0].name").value("Discipline"))
                .andExpect(jsonPath("$.tags[0].categoryName").value("Focus"));
    }

    @Test
    void createEntryWithGoalLink() throws Exception {
        String goalId = createGoalHelper("My Goal");

        String json = """
                {"type":"journal","content":"Goal entry","goalIds":["%s"]}
                """.formatted(goalId);

        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goals.length()").value(1))
                .andExpect(jsonPath("$.goals[0].title").value("My Goal"));
    }

    @Test
    void updateEntryTags() throws Exception {
        String entryId = createEntry("journal", "Entry",  5,  5);
        String catId = createCategoryHelper("Health");
        String valueId = createValueHelper(catId, "Fitness");

        String json = """
                {"valueIds":["%s"]}
                """.formatted(valueId);

        mockMvc.perform(put("/api/log-entries/" + entryId + "/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0].name").value("Fitness"));
    }

    @Test
    void listEntriesIncludesTags() throws Exception {
        String catId = createCategoryHelper("Career");
        String valueId = createValueHelper(catId, "Growth");

        String json = """
                {"type":"journal","content":"With tags","tagValueIds":["%s"]}
                """.formatted(valueId);
        mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/log-entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tags.length()").value(1))
                .andExpect(jsonPath("$.content[0].tags[0].name").value("Growth"));
    }

    private String createEntry(String type, String content, Integer mood, Integer energy) throws Exception {
        CreateLogEntryRequest request = new CreateLogEntryRequest(type, content, mood, energy, null, null, null);
        String body = mockMvc.perform(post("/api/log-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private String createCategoryHelper(String name) throws Exception {
        String json = """
                {"name":"%s","color":"#FF5733"}
                """.formatted(name);
        String body = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createValueHelper(String categoryId, String name) throws Exception {
        String json = """
                {"name":"%s"}
                """.formatted(name);
        String body = mockMvc.perform(post("/api/categories/" + categoryId + "/values")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
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
