package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext.AiSettingsSnapshot;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.goal.GoalRepository;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import com.nhavronskyi.progressmebackend.log.LogEntryRepository;
import com.nhavronskyi.progressmebackend.persona.Persona;
import com.nhavronskyi.progressmebackend.persona.PersonaRepository;
import com.nhavronskyi.progressmebackend.value.UserValue;
import com.nhavronskyi.progressmebackend.value.UserValueRepository;
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

    private final PersonaRepository personaRepository;
    private final AiSettingsRepository aiSettingsRepository;
    private final UserValueRepository valueRepository;
    private final GoalRepository goalRepository;
    private final LogEntryRepository logEntryRepository;
    private final AiProperties properties;

    @Transactional(readOnly = true)
    public AiContext buildContext(UUID userId) {
        Persona persona = personaRepository.findByUserId(userId).orElse(null);

        AiSettingsSnapshot settings = aiSettingsRepository.findByUserId(userId)
                .map(s -> new AiSettingsSnapshot(
                        ResponseStyleResolver.TONE_OVERLAY.fromString(s.getTone()), s.getStrictness(), s.getFocusAreas(),
                        s.getIgnoreAreas(), s.getCustomInstructions(), s.getLanguage()))
                .orElse(new AiSettingsSnapshot(ResponseStyleResolver.TONE_OVERLAY.BALANCED, 5, List.of(), List.of(), null, "en"));

        List<UserValue> values = valueRepository.findByUserIdOrderBySortOrderAsc(userId);

        List<Goal> activeGoals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "active");

        List<LogEntry> recentEntries = logEntryRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("userId"), userId),
                PageRequest.of(0, properties.maxContextEntries(), Sort.by(Sort.Direction.DESC, "loggedAt"))
        ).getContent();

        return new AiContext(persona, settings, values, activeGoals, recentEntries, ResponseStyleResolver.TYPE_STRUCTURE.ANALYSIS);
    }
}
