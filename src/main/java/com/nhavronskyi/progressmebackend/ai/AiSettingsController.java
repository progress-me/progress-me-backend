package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.dto.AiSettingsResponse;
import com.nhavronskyi.progressmebackend.ai.dto.UpdateAiSettingsRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/ai-settings")
@RequiredArgsConstructor
public class AiSettingsController {

    private final AiSettingsService service;

    @GetMapping
    public ResponseEntity<AiSettingsResponse> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.get(user.getId()));
    }

    @PutMapping
    public ResponseEntity<AiSettingsResponse> update(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UpdateAiSettingsRequest request
    ) {
        return ResponseEntity.ok(service.update(user.getId(), request));
    }
}
