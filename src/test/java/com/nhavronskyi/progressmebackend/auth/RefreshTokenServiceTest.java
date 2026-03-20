package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.common.InvalidRefreshTokenException;
import com.nhavronskyi.progressmebackend.user.User;
import com.nhavronskyi.progressmebackend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("refresh-test@example.com");
        user.setPasswordHash("$2a$10$dummyhash");
        user.setName("Test User");
        testUser = userRepository.save(user);
    }

    @Test
    void createRefreshToken_returnsValidToken() {
        RefreshToken token = refreshTokenService.createRefreshToken(testUser);

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(token.getFamilyId()).isNotNull();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
    }

    @Test
    void rotateRefreshToken_returnsNewToken() {
        RefreshToken original = refreshTokenService.createRefreshToken(testUser);

        RefreshToken rotated = refreshTokenService.rotateRefreshToken(original.getToken());

        assertThat(rotated.getToken()).isNotEqualTo(original.getToken());
        assertThat(rotated.getFamilyId()).isEqualTo(original.getFamilyId());
        assertThat(rotated.isRevoked()).isFalse();

        RefreshToken oldToken = refreshTokenRepository.findById(original.getId()).orElseThrow();
        assertThat(oldToken.isRevoked()).isTrue();
    }

    @Test
    void rotateRefreshToken_withInvalidToken_throws() {
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("nonexistent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRefreshToken_reuseAfterGracePeriod_revokesAllUserTokens() {
        RefreshToken original = refreshTokenService.createRefreshToken(testUser);
        String originalTokenValue = original.getToken();

        // First rotation succeeds
        RefreshToken successor = refreshTokenService.rotateRefreshToken(originalTokenValue);

        // Simulate grace period expiry by backdating the successor's created_at
        successor.setCreatedAt(successor.getCreatedAt().minusSeconds(30));
        refreshTokenRepository.save(successor);

        // Reuse of rotated token should revoke all and throw
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(originalTokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("reuse detected");
    }

    @Test
    void rotateRefreshToken_reuseWithinGracePeriod_returnsSuccessor() {
        RefreshToken original = refreshTokenService.createRefreshToken(testUser);
        String originalTokenValue = original.getToken();

        // First rotation succeeds
        RefreshToken successor = refreshTokenService.rotateRefreshToken(originalTokenValue);

        // Immediate reuse (within 10s grace) should return the same successor
        RefreshToken result = refreshTokenService.rotateRefreshToken(originalTokenValue);

        assertThat(result.getToken()).isEqualTo(successor.getToken());
        assertThat(result.getFamilyId()).isEqualTo(original.getFamilyId());
    }

    @Test
    void rotateRefreshToken_expiredToken_throws() {
        RefreshToken original = refreshTokenService.createRefreshToken(testUser);

        // Simulate expired token
        original.setExpiresAt(OffsetDateTime.now().minusHours(1));
        refreshTokenRepository.save(original);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(original.getToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeAllUserTokens_revokesAll() {
        refreshTokenService.createRefreshToken(testUser);
        refreshTokenService.createRefreshToken(testUser);

        refreshTokenService.revokeAllUserTokens(testUser.getId());

        long activeCount = refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(testUser.getId()))
                .filter(rt -> !rt.isRevoked())
                .count();
        assertThat(activeCount).isZero();
    }
}
