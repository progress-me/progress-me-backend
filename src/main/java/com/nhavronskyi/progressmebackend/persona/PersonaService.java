package com.nhavronskyi.progressmebackend.persona;

import com.nhavronskyi.progressmebackend.persona.dto.PersonaResponse;
import com.nhavronskyi.progressmebackend.persona.dto.UpdatePersonaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository repository;

    @Transactional
    public PersonaResponse get(UUID userId) {
        return PersonaResponse.from(
                repository.findByUserId(userId).orElseGet(() -> createEmpty(userId))
        );
    }

    @Transactional
    public PersonaResponse update(UUID userId, UpdatePersonaRequest request) {
        Persona persona = repository.findByUserId(userId).orElseGet(() -> createEmpty(userId));
        persona.setDisplayName(request.displayName());
        persona.setAge(request.age());
        persona.setBio(request.bio());
        persona.setStrengths(request.strengths());
        persona.setWeaknesses(request.weaknesses());
        persona.setInterests(request.interests());
        persona.setContext(request.context());
        return PersonaResponse.from(repository.save(persona));
    }

    private Persona createEmpty(UUID userId) {
        Persona persona = new Persona();
        persona.setUserId(userId);
        return repository.save(persona);
    }
}
