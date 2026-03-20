package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.dto.AiSettingsResponse;
import com.nhavronskyi.progressmebackend.ai.dto.UpdateAiSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiSettingsService {

    private static final AiSettingsResponse DEFAULTS = new AiSettingsResponse(
            null, "balanced", 5, List.of(), List.of(), null, "en", null
    );

    private final AiSettingsRepository repository;

    @Cacheable(value = "ai-settings", key = "#userId")
    @Transactional(readOnly = true)
    public AiSettingsResponse get(UUID userId) {
        return repository.findByUserId(userId)
                .map(AiSettingsResponse::from)
                .orElse(DEFAULTS);
    }

    @CacheEvict(value = "ai-settings", key = "#userId")
    @Transactional
    public AiSettingsResponse update(UUID userId, UpdateAiSettingsRequest request) {
        AiSettings settings = repository.findByUserId(userId).orElseGet(() -> {
            AiSettings aiSettings = new AiSettings();
            aiSettings.setUserId(userId);
            return aiSettings;
        });

        settings.setTone(request.tone() != null ? request.tone() : "balanced");
        settings.setStrictness(request.strictness() != null ? request.strictness() : 5);
        settings.setFocusAreas(request.focusAreas() != null ? request.focusAreas() : List.of());
        settings.setIgnoreAreas(request.ignoreAreas() != null ? request.ignoreAreas() : List.of());
        settings.setCustomInstructions(request.customInstructions());
        settings.setLanguage(request.language() != null ? request.language() : "en");

        return AiSettingsResponse.from(repository.save(settings));
    }
}
