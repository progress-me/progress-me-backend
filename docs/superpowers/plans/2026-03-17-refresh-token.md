# Refresh Token Authentication Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add refresh token rotation with theft detection to enable automatic session renewal.

**Architecture:** Short-lived JWT access tokens (15 min) paired with DB-stored opaque refresh tokens (30 days). Refresh tokens rotate on each use with a 10-second grace period for concurrent requests. Reuse outside the grace period revokes all user tokens.

**Tech Stack:** Spring Boot 4.0, Spring Security, jjwt 0.12.6, PostgreSQL, Flyway, JUnit 5, MockMvc, H2 (tests)

**Spec:** `docs/superpowers/specs/2026-03-17-refresh-token-design.md`

---

## File Structure

| Action | Path                                                         | Responsibility                                        |
| ------ | ------------------------------------------------------------ | ----------------------------------------------------- |
| Create | `src/main/resources/db/migration/V8__refresh_tokens.sql`     | Flyway migration for refresh_tokens table             |
| Create | `src/main/java/.../auth/RefreshToken.java`                   | JPA entity                                            |
| Create | `src/main/java/.../auth/RefreshTokenRepository.java`         | Data access                                           |
| Create | `src/main/java/.../auth/RefreshTokenService.java`            | Token creation, rotation, revocation, cleanup         |
| Create | `src/main/java/.../auth/dto/RefreshRequest.java`             | Request DTO                                           |
| Create | `src/main/java/.../common/InvalidRefreshTokenException.java` | Custom exception                                      |
| Modify | `src/main/java/.../auth/dto/AuthResponse.java`               | Add `refreshToken` field                              |
| Modify | `src/main/java/.../auth/AuthService.java`                    | Wire refresh token into login/register/refresh/logout |
| Modify | `src/main/java/.../auth/AuthController.java`                 | Add refresh + logout endpoints                        |
| Modify | `src/main/java/.../config/SecurityConfig.java`               | Explicit public paths instead of wildcard             |
| Modify | `src/main/java/.../common/GlobalExceptionHandler.java`       | Handle InvalidRefreshTokenException                   |
| Modify | `src/main/resources/application.yaml`                        | Update expiration, add refresh config                 |
| Modify | `src/test/resources/application.yaml`                        | Add refresh-expiration-ms for tests                   |
| Create | `src/test/java/.../auth/RefreshTokenServiceTest.java`        | Unit tests for rotation/theft detection               |
| Modify | `src/test/java/.../auth/AuthControllerTest.java`             | Integration tests for refresh/logout flows            |

Base package: `com.nhavronskyi.progressmebackend`

---

### Task 1: Database Migration

**Files:**

- Create: `src/main/resources/db/migration/V8__refresh_tokens.sql`

- [ ] **Step 1: Write the migration**

```sql
CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE  NOT NULL,
    family_id  UUID        NOT NULL,
    revoked    BOOLEAN              DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ          DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
```

- [ ] **Step 2: Verify migration applies**

Run: `./gradlew test --tests "com.nhavronskyi.progressmebackend.ProgressMeBackendApplicationTests"` (context loads = Flyway runs)
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V8__refresh_tokens.sql
git commit -m "feat(auth): add refresh_tokens table migration"
```

---

### Task 2: RefreshToken Entity + Repository

**Files:**

- Create: `src/main/java/.../auth/RefreshToken.java`
- Create: `src/main/java/.../auth/RefreshTokenRepository.java`

- [ ] **Step 1: Create RefreshToken entity**

```java
package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
```

- [ ] **Step 2: Create RefreshTokenRepository**

```java
package com.nhavronskyi.progressmebackend.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId AND rt.revoked = false")
    int revokeAllByFamilyId(UUID familyId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.familyId = :familyId AND rt.revoked = false")
    Optional<RefreshToken> findActiveByFamilyId(UUID familyId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevoked(OffsetDateTime now);
}
```

- [ ] **Step 3: Verify context still loads**

Run: `./gradlew test --tests "com.nhavronskyi.progressmebackend.ProgressMeBackendApplicationTests"`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/nhavronskyi/progressmebackend/auth/RefreshToken.java \
        src/main/java/com/nhavronskyi/progressmebackend/auth/RefreshTokenRepository.java
git commit -m "feat(auth): add RefreshToken entity and repository"
```

---

### Task 3: InvalidRefreshTokenException + Exception Handler

**Files:**

- Create: `src/main/java/.../common/InvalidRefreshTokenException.java`
- Modify: `src/main/java/.../common/GlobalExceptionHandler.java`

- [ ] **Step 1: Create exception class**

```java
package com.nhavronskyi.progressmebackend.common;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Add handler to GlobalExceptionHandler**

Add this method to `GlobalExceptionHandler.java` (before the `BadCredentialsException` handler, around line 30):

```java
@ExceptionHandler(InvalidRefreshTokenException.class)
public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/nhavronskyi/progressmebackend/common/InvalidRefreshTokenException.java \
        src/main/java/com/nhavronskyi/progressmebackend/common/GlobalExceptionHandler.java
git commit -m "feat(auth): add InvalidRefreshTokenException with 401 handler"
```

---

### Task 4: RefreshTokenService (Core Logic)

**Files:**

- Create: `src/main/java/.../auth/RefreshTokenService.java`
- Create: `src/test/java/.../auth/RefreshTokenServiceTest.java`

- [ ] **Step 1: Write failing tests for RefreshTokenService**

```java
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        refreshTokenService.rotateRefreshToken(originalTokenValue);

        // Simulate grace period expiry by updating created_at of the old token
        RefreshToken oldToken = refreshTokenRepository.findById(original.getId()).orElseThrow();
        oldToken.setCreatedAt(oldToken.getCreatedAt().minusSeconds(30));
        refreshTokenRepository.save(oldToken);

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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.nhavronskyi.progressmebackend.auth.RefreshTokenServiceTest"`
Expected: FAIL (RefreshTokenService does not exist yet)

- [ ] **Step 3: Implement RefreshTokenService**

```java
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
        boolean withinGracePeriod = revokedToken.getCreatedAt()
                .plusSeconds(GRACE_PERIOD_SECONDS)
                .isAfter(OffsetDateTime.now());

        if (withinGracePeriod) {
            return refreshTokenRepository.findActiveByFamilyId(revokedToken.getFamilyId())
                    .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));
        }

        refreshTokenRepository.revokeAllByUserId(revokedToken.getUser().getId());
        throw new InvalidRefreshTokenException("Token reuse detected. All sessions revoked.");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.nhavronskyi.progressmebackend.auth.RefreshTokenServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/nhavronskyi/progressmebackend/auth/RefreshTokenService.java \
        src/test/java/com/nhavronskyi/progressmebackend/auth/RefreshTokenServiceTest.java
git commit -m "feat(auth): implement RefreshTokenService with rotation and theft detection"
```

---

### Task 5: RefreshRequest DTO and Configuration

**Files:**

- Create: `src/main/java/.../auth/dto/RefreshRequest.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/test/resources/application.yaml`

- [ ] **Step 1: Create RefreshRequest DTO**

```java
package com.nhavronskyi.progressmebackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
```

- [ ] **Step 2: Update application.yaml**

Change the `app.jwt` section to:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1kZXZlbG9wbWVudC1vbmx5LXBsZWFzZS1jaGFuZ2UtaW4tcHJvZA==}
    expiration-ms: 900000
    refresh-expiration-ms: 2592000000
```

- [ ] **Step 4: Update test application.yaml**

Add `refresh-expiration-ms` to `src/test/resources/application.yaml` under `app.jwt`:

```yaml
app:
  jwt:
    secret: dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbXVzdC1iZS1sb25nLWVub3VnaA==
    expiration-ms: 900000
    refresh-expiration-ms: 2592000000
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/nhavronskyi/progressmebackend/auth/dto/RefreshRequest.java \
        src/main/resources/application.yaml \
        src/test/resources/application.yaml
git commit -m "feat(auth): add RefreshRequest DTO and update config for refresh tokens"
```

---

### Task 6: Update AuthResponse + AuthService + AuthController + SecurityConfig

**Files:**

- Modify: `src/main/java/.../auth/dto/AuthResponse.java`
- Modify: `src/main/java/.../auth/AuthService.java`
- Modify: `src/main/java/.../auth/AuthController.java`
- Modify: `src/main/java/.../config/SecurityConfig.java`
- Modify: `src/main/java/.../ProgressMeBackendApplication.java`

**Note:** AuthResponse and AuthService must be updated together to avoid a compile break.

- [ ] **Step 1: Update AuthResponse to include refreshToken**

Change `AuthResponse.java` to:

```java
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
```

- [ ] **Step 2: Update AuthService**

Add `RefreshTokenService` dependency and update methods. Full updated file:

```java
package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.LoginRequest;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.persona.Persona;
import com.nhavronskyi.progressmebackend.persona.PersonaRepository;
import com.nhavronskyi.progressmebackend.user.User;
import com.nhavronskyi.progressmebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new com.nhavronskyi.progressmebackend.common.EmailAlreadyExistsException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user = userRepository.save(user);

        Persona persona = new Persona();
        persona.setUserId(user.getId());
        personaRepository.save(persona);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user.getId()), refreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user.getId()), refreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenValue);
        User user = newRefreshToken.getUser();
        return new AuthResponse(jwtService.generateToken(user.getId()), newRefreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
```

- [ ] **Step 3: Update AuthController**

Add refresh and logout endpoints:

```java
package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.LoginRequest;
import com.nhavronskyi.progressmebackend.auth.dto.RefreshRequest;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal User user) {
        authService.logout(user.getId());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName()
        ));
    }
}
```

- [ ] **Step 4: Update SecurityConfig — replace wildcard with explicit paths**

Change the `requestMatchers` line in `SecurityConfig.java` from:

```java
.requestMatchers("/api/auth/**", "/actuator/health").permitAll()
```

to:

```java
.requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/actuator/health").permitAll()
```

- [ ] **Step 5: Enable scheduling**

Add `@EnableScheduling` to the main application class `ProgressMeBackendApplication.java`:

```java
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProgressMeBackendApplication {
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nhavronskyi/progressmebackend/auth/dto/AuthResponse.java \
        src/main/java/com/nhavronskyi/progressmebackend/auth/AuthService.java \
        src/main/java/com/nhavronskyi/progressmebackend/auth/AuthController.java \
        src/main/java/com/nhavronskyi/progressmebackend/config/SecurityConfig.java \
        src/main/java/com/nhavronskyi/progressmebackend/ProgressMeBackendApplication.java
git commit -m "feat(auth): wire refresh token into auth flow with refresh/logout endpoints"
```

---

### Task 7: Update Existing Tests + Add Integration Tests

**Files:**

- Modify: `src/test/java/.../auth/AuthControllerTest.java`

- [ ] **Step 1: Update existing tests to expect refreshToken in response**

In `AuthControllerTest.java`, add `.andExpect(jsonPath("$.refreshToken").isNotEmpty())` to the `registerAndLogin` test assertions (lines 36 and 48).

- [ ] **Step 2: Add refresh flow integration test**

Add to `AuthControllerTest.java`:

```java
@Test
void refreshTokenFlow() throws Exception {
    // Register to get tokens
    RegisterRequest register = new RegisterRequest("refresh@example.com", "password123", "Refresh User");
    String body = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(register)))
            .andReturn().getResponse().getContentAsString();

    String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

    // Use refresh token to get new tokens
    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value("refresh@example.com"));
}
```

- [ ] **Step 3: Add invalid refresh token test**

```java
@Test
void refreshWithInvalidTokenFails() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"))))
            .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 4: Add logout test**

```java
@Test
void logoutRevokesRefreshTokens() throws Exception {
    // Register
    RegisterRequest register = new RegisterRequest("logout@example.com", "password123", "Logout User");
    String body = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(register)))
            .andReturn().getResponse().getContentAsString();

    String accessToken = objectMapper.readTree(body).get("token").asText();
    String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

    // Logout
    mockMvc.perform(post("/api/auth/logout")
                    .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());

    // Refresh should fail after logout
    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 5: Add logout requires auth test**

```java
@Test
void logoutRequiresAuth() throws Exception {
    mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 6: Run all tests**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/nhavronskyi/progressmebackend/auth/AuthControllerTest.java
git commit -m "test(auth): add integration tests for refresh token and logout flows"
```

---

### Task 8: Final Verification

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 2: Start the app and smoke test manually (optional)**

Run: `./gradlew bootRun`

Test with curl:

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","name":"Test"}' | jq .

# Refresh (use refreshToken from above)
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<token>"}' | jq .
```

- [ ] **Step 3: Commit any final fixes if needed**
