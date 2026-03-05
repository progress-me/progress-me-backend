package com.nhavronskyi.progressmebackend.log;

import com.nhavronskyi.progressmebackend.log.dto.CreateLogEntryRequest;
import com.nhavronskyi.progressmebackend.log.dto.LogEntryResponse;
import com.nhavronskyi.progressmebackend.log.dto.UpdateEntryTagsRequest;
import com.nhavronskyi.progressmebackend.log.dto.UpdateLogEntryRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/log-entries")
@RequiredArgsConstructor
public class LogEntryController {

    private final LogEntryService service;

    @PostMapping
    public ResponseEntity<LogEntryResponse> create(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateLogEntryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<LogEntryResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID goalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(user.getId(), type, goalId, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogEntryResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.get(user.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogEntryResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateLogEntryRequest request
    ) {
        return ResponseEntity.ok(service.update(user.getId(), id, request));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<LogEntryResponse> updateTags(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateEntryTagsRequest request
    ) {
        return ResponseEntity.ok(service.updateTags(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
