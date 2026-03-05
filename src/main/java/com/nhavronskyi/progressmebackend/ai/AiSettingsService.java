package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.dto.AiSettingsResponse;
import com.nhavronskyi.progressmebackend.ai.dto.UpdateAiSettingsRequest;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public AiSettingsResponse get(UUID userId) {
        return repository.findByUserId(userId)
                .map(AiSettingsResponse::from)
                .orElse(DEFAULTS);
    }

    @Transactional
    public AiSettingsResponse update(UUID userId, UpdateAiSettingsRequest request) {
        AiSettings settings = repository.findByUserId(userId).orElseGet(() -> {
            AiSettings aiSettings = new AiSettings();
            aiSettings.setUserId(userId);
            return aiSettings;
        });

        if (request.tone() != null){
            settings.setTone(request.tone());
        }
        if (request.strictness() != null){
            settings.setStrictness(request.strictness());
        }
        if (request.focusAreas() != null){
            settings.setFocusAreas(request.focusAreas());
        }
        if (request.ignoreAreas() != null){
            settings.setIgnoreAreas(request.ignoreAreas());
        }
        if (request.customInstructions() != null){
            settings.setCustomInstructions(request.customInstructions());
        }
        if (request.language() != null){
            settings.setLanguage(request.language());
        }

        return AiSettingsResponse.from(repository.save(settings));
    }
}
