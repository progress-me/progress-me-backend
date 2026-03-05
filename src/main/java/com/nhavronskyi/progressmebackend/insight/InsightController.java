package com.nhavronskyi.progressmebackend.insight;

import com.nhavronskyi.progressmebackend.ai.PrebuiltPrompt;
import com.nhavronskyi.progressmebackend.ai.dto.PrebuiltPromptResponse;
import com.nhavronskyi.progressmebackend.insight.dto.AnalyzeRequest;
import com.nhavronskyi.progressmebackend.insight.dto.InsightResponse;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService service;

    @GetMapping("/prompts")
    public ResponseEntity<List<PrebuiltPromptResponse>> getPrompts() {
        return ResponseEntity.ok(
                Arrays.stream(PrebuiltPrompt.values())
                        .map(PrebuiltPromptResponse::from)
                        .toList()
        );
    }

    @PostMapping("/analyze")
    public ResponseEntity<InsightResponse> analyze(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid AnalyzeRequest request
    ) {
        return ResponseEntity.ok(service.analyze(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<InsightResponse>> list(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(user.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsightResponse> getById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.getById(user.getId(), id));
    }
}
