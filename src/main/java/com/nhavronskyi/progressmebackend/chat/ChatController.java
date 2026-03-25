package com.nhavronskyi.progressmebackend.chat;

import com.nhavronskyi.progressmebackend.ai.PrebuiltPrompt;
import com.nhavronskyi.progressmebackend.ai.dto.PrebuiltPromptResponse;
import com.nhavronskyi.progressmebackend.chat.dto.ChatMessageResponse;
import com.nhavronskyi.progressmebackend.chat.dto.ChatSessionResponse;
import com.nhavronskyi.progressmebackend.chat.dto.CreateSessionRequest;
import com.nhavronskyi.progressmebackend.chat.dto.SendMessageRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/starters")
    public ResponseEntity<List<PrebuiltPromptResponse>> getStarters() {
        return ResponseEntity.ok(
                Arrays.stream(PrebuiltPrompt.values())
                        .map(PrebuiltPromptResponse::from)
                        .toList()
        );
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createSession(user.getId(), request.message()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<ChatSessionResponse>> listSessions(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        return ResponseEntity.ok(chatService.getSessions(user.getId(), pageable));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getSessionHistory(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(chatService.getSessionHistory(user.getId(), sessionId, pageable));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @RequestBody @Valid SendMessageRequest request
    ) {
        return ResponseEntity.ok(chatService.sendMessage(user.getId(), sessionId, request.message()));
    }
}
