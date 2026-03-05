package com.nhavronskyi.progressmebackend.persona;

import com.nhavronskyi.progressmebackend.persona.dto.PersonaResponse;
import com.nhavronskyi.progressmebackend.persona.dto.UpdatePersonaRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService service;

    @GetMapping("/persona")
    public ResponseEntity<PersonaResponse> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.get(user.getId()));
    }

    @PutMapping("/persona")
    public ResponseEntity<PersonaResponse> update(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UpdatePersonaRequest request
    ) {
        return ResponseEntity.ok(service.update(user.getId(), request));
    }
}
