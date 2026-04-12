package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext.AiSettingsSnapshot;
import com.nhavronskyi.progressmebackend.ai.dto.AiSettingsResponse;
import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.category.CategoryRepository;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.goal.GoalRepository;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import com.nhavronskyi.progressmebackend.log.LogEntryRepository;
import com.nhavronskyi.progressmebackend.persona.PersonaService;
import com.nhavronskyi.progressmebackend.persona.dto.PersonaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiContextService {

    private final PersonaService personaService;
    private final AiSettingsService aiSettingsService;
    private final CategoryRepository categoryRepository;
    private final GoalRepository goalRepository;
    private final LogEntryRepository logEntryRepository;
    private final AiProperties properties;

    @Transactional(readOnly = true)
    public AiContext buildContext(UUID userId) {
        PersonaResponse persona = personaService.get(userId);

        AiSettingsResponse settingsResponse = aiSettingsService.get(userId);
        AiSettingsSnapshot settings = new AiSettingsSnapshot(
                ResponseStyleResolver.TONE_OVERLAY.fromString(settingsResponse.tone()),
                settingsResponse.strictness(),
                settingsResponse.focusAreas(),
                settingsResponse.ignoreAreas(),
                settingsResponse.customInstructions(),
                settingsResponse.language()
        );

        List<Category> categories = categoryRepository.findByUserIdOrderBySortOrderAsc(userId);

        List<Goal> activeGoals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "active");

        List<LogEntry> recentEntries = logEntryRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("userId"), userId),
                PageRequest.of(0, properties.maxContextEntries(), Sort.by(Sort.Direction.DESC, "loggedAt"))
        ).getContent();

        return new AiContext(persona, settings, categories, activeGoals, recentEntries);
    }
}
