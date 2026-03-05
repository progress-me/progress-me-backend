package com.nhavronskyi.progressmebackend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateAndExtract() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        Optional<UUID> extracted = jwtService.extractUserIdIfValid(token);

        assertThat(extracted).contains(userId);
    }

    @Test
    void invalidTokenReturnsEmpty() {
        assertThat(jwtService.extractUserIdIfValid("garbage")).isEmpty();
    }

    @Test
    void tamperedTokenReturnsEmpty() {
        String token = jwtService.generateToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.extractUserIdIfValid(tampered)).isEmpty();
    }
}
