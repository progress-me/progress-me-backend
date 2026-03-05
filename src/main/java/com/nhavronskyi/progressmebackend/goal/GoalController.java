package com.nhavronskyi.progressmebackend.goal;

import com.nhavronskyi.progressmebackend.goal.dto.CreateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.GoalResponse;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalStatusRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @PostMapping
    public ResponseEntity<GoalResponse> create(@AuthenticationPrincipal User user,
                                                @RequestBody @Valid CreateGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(@AuthenticationPrincipal User user,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) UUID categoryId) {
        // Ensure default goal exists on first access
        service.ensureDefaultGoal(user.getId());
        return ResponseEntity.ok(service.list(user.getId(), status, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> get(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(user.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@AuthenticationPrincipal User user,
                                                @PathVariable UUID id,
                                                @RequestBody @Valid UpdateGoalRequest request) {
        return ResponseEntity.ok(service.update(user.getId(), id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<GoalResponse> updateStatus(@AuthenticationPrincipal User user,
                                                      @PathVariable UUID id,
                                                      @RequestBody @Valid UpdateGoalStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
