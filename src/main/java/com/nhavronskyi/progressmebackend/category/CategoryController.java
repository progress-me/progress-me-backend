package com.nhavronskyi.progressmebackend.category;

import com.nhavronskyi.progressmebackend.category.dto.CategoryResponse;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.category.dto.UpdateCategoryRequest;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal User user,
                                                    @RequestBody @Valid CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.listForUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(@AuthenticationPrincipal User user,
                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(user.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal User user,
                                                    @PathVariable UUID id,
                                                    @RequestBody @Valid UpdateCategoryRequest request) {
        return ResponseEntity.ok(service.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        service.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
