# Refresh Token Authentication Design

## Overview

Add refresh token support to automate user session refresh. Short-lived access tokens (15 min JWT) paired with long-lived refresh tokens (30 days, DB-stored) with rotation and theft detection.

## Architecture

- **Access token**: Short-lived JWT (15 min), stateless, used for API auth (no change to validation logic)
- **Refresh token**: Opaque token (32 bytes from `SecureRandom`, Base64url-encoded) stored in PostgreSQL (30 days), rotated on each use
- **Token family**: Each login creates a family (`familyId`). All tokens descended from one login share a `familyId`. If a previously-rotated token is reused, all tokens for that user are revoked (theft detection)
- **Race condition mitigation**: `rotateRefreshToken` uses `SELECT ... FOR UPDATE` on the token row. If the token was already rotated within a 10-second grace period, return the same successor token pair instead of triggering theft detection

## Database Schema

New Flyway migration adding `refresh_tokens` table:

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    family_id UUID NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
```

Note: The `UNIQUE` constraint on `token` already creates an index, so no separate index needed.

## Configuration

New properties in `application.yaml`:

```yaml
app:
  jwt:
    expiration-ms: 900000 # 15 minutes (changed from 86400000)
    refresh-expiration-ms: 2592000000 # 30 days (must be bound to long, not int)
```

## API Changes

### Modified Endpoints

**POST /api/auth/register** and **POST /api/auth/login**

- Response now includes `refreshToken` field (breaking change to `AuthResponse` record — JSON field-name-based serialization is unaffected)

### New Endpoints

**POST /api/auth/refresh**

- Public (no auth required)
- Request body: `RefreshRequest { @NotBlank String refreshToken }`
- Success: returns new `AuthResponse` (new access token + new refresh token)
- Failure: 401 Unauthorized

**POST /api/auth/logout**

- Authenticated (requires valid access token)
- Revokes all refresh tokens for the authenticated user
- Returns 204 No Content

### DTOs

```java
public record AuthResponse(
    String token,
    String refreshToken,
    UUID userId,
    String email,
    String name
)

public record RefreshRequest(@NotBlank String refreshToken) {}
```

## Components

### RefreshToken Entity

JPA entity mapping to `refresh_tokens` table with fields: `id`, `user`, `token`, `familyId`, `revoked`, `expiresAt`, `createdAt`.

### RefreshTokenRepository

Spring Data JPA repository with methods:

- `findByTokenForUpdate(String token)`: `@Lock(PESSIMISTIC_WRITE)` query for rotation serialization
- `findByToken(String token): Optional<RefreshToken>` (read-only lookups)
- `revokeAllByUserId(UUID userId)`: update query setting `revoked = true`
- `revokeAllByFamilyId(UUID familyId)`: update query setting `revoked = true`
- `deleteExpiredAndRevoked(OffsetDateTime now)`: cleanup expired OR revoked tokens

### RefreshTokenService

Core service handling:

- `createRefreshToken(User user)`: generates 32-byte `SecureRandom` token (Base64url-encoded), creates new `familyId`, stores in DB
- `rotateRefreshToken(String token)`: `@Transactional` — acquires pessimistic lock on token row, validates not expired. If revoked: checks if rotated within 10s grace period (return successor), otherwise revokes ALL user tokens and throws. If valid: creates new token in same family, revokes old one
- `revokeAllUserTokens(UUID userId)`: revokes all tokens for logout

### AuthService Changes

- `register()` and `login()`: also create refresh token, return in response
- New `refresh(String refreshToken)`: delegates to RefreshTokenService, issues new access JWT
- New `logout(UUID userId)`: delegates to RefreshTokenService

### AuthController Changes

- `register()` and `login()`: return updated AuthResponse with refresh token
- New `POST /api/auth/refresh` endpoint
- New `POST /api/auth/logout` endpoint

### SecurityConfig Changes

Replace `/api/auth/**` wildcard with explicit paths to ensure logout requires authentication:

```java
.requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
```

## Authentication Flow

1. **Login/Register**: Client receives access JWT (15 min) + refresh token (30 days)
2. **API calls**: Client sends access JWT in `Authorization: Bearer` header (unchanged)
3. **Access token expires**: Client gets 401, calls `POST /api/auth/refresh` with refresh token
4. **Refresh succeeds**: New access JWT + new refresh token returned, old refresh token revoked
5. **Concurrent refresh (grace period)**: If same token is refreshed again within 10s, return the same successor pair
6. **Refresh token reuse detected (outside grace)**: All user tokens revoked, user must re-login
7. **Logout**: All refresh tokens for user revoked

## Error Handling

| Scenario                         | HTTP Status | Error                                         |
| -------------------------------- | ----------- | --------------------------------------------- |
| Invalid/expired refresh token    | 401         | "Invalid or expired refresh token"            |
| Reused (rotated) refresh token   | 401         | "Token reuse detected. All sessions revoked." |
| Missing refresh token in request | 400         | Validation error                              |

## Cleanup

A `@Scheduled` method in RefreshTokenService runs daily to delete tokens where `expires_at < now() OR revoked = true`.

## Testing Strategy

- Unit tests for `RefreshTokenService`: happy path rotation, expired token, revoked token (theft detection), grace period concurrent refresh
- Integration tests: full refresh flow (login → use → refresh → use new token), theft detection scenario, logout revokes all tokens
