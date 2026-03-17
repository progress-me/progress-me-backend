# Refresh Token Authentication Design

## Overview

Add refresh token support to automate user session refresh. Short-lived access tokens (15 min JWT) paired with long-lived refresh tokens (30 days, DB-stored) with rotation and theft detection.

## Architecture

- **Access token**: Short-lived JWT (15 min), stateless, used for API auth (no change to validation logic)
- **Refresh token**: Opaque UUID stored in PostgreSQL (30 days), rotated on each use
- **Token family**: Each login creates a family (`familyId`). All tokens descended from one login share a `familyId`. If a previously-rotated token is reused, all tokens for that user are revoked (theft detection)

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

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

## Configuration

New properties in `application.yaml`:

```yaml
app:
  jwt:
    expiration-ms: 900000 # 15 minutes (changed from 86400000)
    refresh-expiration-ms: 2592000000 # 30 days
```

## API Changes

### Modified Endpoints

**POST /api/auth/register** and **POST /api/auth/login**

- Response now includes `refreshToken` field

### New Endpoints

**POST /api/auth/refresh**

- Public (no auth required)
- Request body: `{ "refreshToken": "..." }`
- Success: returns new `AuthResponse` (new access token + new refresh token)
- Failure: 401 Unauthorized

**POST /api/auth/logout**

- Authenticated (requires valid access token)
- Revokes all refresh tokens for the authenticated user
- Returns 204 No Content

### Response DTO

```java
public record AuthResponse(
    String token,
    String refreshToken,
    UUID userId,
    String email,
    String name
)
```

## Components

### RefreshToken Entity

JPA entity mapping to `refresh_tokens` table with fields: `id`, `user`, `token`, `familyId`, `revoked`, `expiresAt`, `createdAt`.

### RefreshTokenRepository

Spring Data JPA repository with methods:

- `findByToken(String token): Optional<RefreshToken>`
- `revokeAllByUserId(UUID userId)`: update query setting `revoked = true`
- `revokeAllByFamilyId(UUID familyId)`: update query setting `revoked = true`
- `deleteByExpiresAtBefore(OffsetDateTime now)`: cleanup expired tokens

### RefreshTokenService

Core service handling:

- `createRefreshToken(User user)`: generates new token with new family, stores in DB
- `rotateRefreshToken(String token)`: validates token, creates new token in same family, revokes old one. If token is already revoked (reuse detected), revokes ALL tokens for that user and throws exception
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

- Add `/api/auth/refresh` to public endpoints
- `/api/auth/logout` requires authentication (already covered by default)

## Authentication Flow

1. **Login/Register**: Client receives access JWT (15 min) + refresh token (30 days)
2. **API calls**: Client sends access JWT in `Authorization: Bearer` header (unchanged)
3. **Access token expires**: Client gets 401, calls `POST /api/auth/refresh` with refresh token
4. **Refresh succeeds**: New access JWT + new refresh token returned, old refresh token revoked
5. **Refresh token reuse detected**: All user tokens revoked, user must re-login
6. **Logout**: All refresh tokens for user revoked

## Error Handling

| Scenario                         | HTTP Status | Error                                         |
| -------------------------------- | ----------- | --------------------------------------------- |
| Invalid/expired refresh token    | 401         | "Invalid or expired refresh token"            |
| Reused (rotated) refresh token   | 401         | "Token reuse detected. All sessions revoked." |
| Missing refresh token in request | 400         | Validation error                              |

## Cleanup

Expired refresh tokens should be cleaned up periodically. A `@Scheduled` method in RefreshTokenService runs daily to delete tokens where `expires_at < now()`.
