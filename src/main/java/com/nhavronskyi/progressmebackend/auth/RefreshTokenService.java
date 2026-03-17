package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.common.InvalidRefreshTokenException;
import com.nhavronskyi.progressmebackend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int GRACE_PERIOD_SECONDS = 10;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateSecureToken());
        refreshToken.setFamilyId(UUID.randomUUID());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenForUpdate(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        if (existingToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        if (existingToken.isRevoked()) {
            return handlePotentialReuse(existingToken);
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(existingToken.getUser());
        newToken.setToken(generateSecureToken());
        newToken.setFamilyId(existingToken.getFamilyId());
        newToken.setExpiresAt(existingToken.getExpiresAt());
        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(OffsetDateTime.now());
    }

    private RefreshToken handlePotentialReuse(RefreshToken revokedToken) {
        return refreshTokenRepository.findActiveByFamilyId(revokedToken.getFamilyId())
                .filter(successor -> successor.getCreatedAt()
                        .plusSeconds(GRACE_PERIOD_SECONDS)
                        .isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> {
                    refreshTokenRepository.revokeAllByUserId(revokedToken.getUser().getId());
                    return new InvalidRefreshTokenException("Token reuse detected. All sessions revoked.");
                });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
