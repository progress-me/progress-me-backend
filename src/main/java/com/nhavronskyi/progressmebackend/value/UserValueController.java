package com.nhavronskyi.progressmebackend.value;

import com.nhavronskyi.progressmebackend.user.User;
import com.nhavronskyi.progressmebackend.value.dto.CreateValueRequest;
import com.nhavronskyi.progressmebackend.value.dto.UpdateValueRequest;
import com.nhavronskyi.progressmebackend.value.dto.ValueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserValueController {

    private final UserValueService service;

    @PostMapping("/api/categories/{catId}/values")
    public ResponseEntity<ValueResponse> create(@AuthenticationPrincipal User user,
                                                 @PathVariable UUID catId,
                                                 @RequestBody @Valid CreateValueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(), catId, request));
    }

    @GetMapping("/api/values")
    public ResponseEntity<List<ValueResponse>> listAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.listByUser(user.getId()));
    }

    @PutMapping("/api/values/{id}")
    public ResponseEntity<ValueResponse> update(@AuthenticationPrincipal User user,
                                                 @PathVariable UUID id,
                                                 @RequestBody @Valid UpdateValueRequest request) {
        return ResponseEntity.ok(service.update(user.getId(), id, request));
    }

    @DeleteMapping("/api/values/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
