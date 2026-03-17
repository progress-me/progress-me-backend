package com.nhavronskyi.progressmebackend.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String refreshToken,
        UUID userId,
        String email,
        String name
) {
}
