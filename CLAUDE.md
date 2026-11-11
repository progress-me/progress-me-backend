# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew bootRun                                   # run on :8080 (needs GOOGLE_API_KEY, Postgres, Redis)
./gradlew test                                      # all tests (H2 + simple cache; no Postgres/Redis needed)
./gradlew test --tests '*GoalControllerTest'        # single class
./gradlew test --tests '*GoalControllerTest.methodName'  # single method
./gradlew build                                     # what CI runs
```

Local runtime dependencies (defaults in `src/main/resources/application.yaml`): Postgres `localhost:5432/progress_analyzer` (app/dev), Redis `localhost:6379` (pw `dev`), env `GOOGLE_API_KEY`. Optional: `JWT_SECRET`, `GOOGLE_API_MODEL`, `GOOGLE_API_REASONING_MODEL`. Deployment notes are in `DEPLOYMENT.md` (GHCR image + docker-compose).

## Stack

Java 21, Spring Boot 4.1, Spring AI 2.0 (Google GenAI / Gemini), JPA + Flyway (Postgres), Redis cache, stateless JWT auth, Lombok. Base package: `com.nhavronskyi.progressmebackend`, organized by feature (`goal/`, `log/`, `chat/`, `ai/`, ...), each with controller/service/repository/entity and a `dto/` subpackage (records).

## Architecture

**Chat → orchestrator → task handlers** (the core of the app, under `ai/orchestrator/` and `chat/`):

- `ChatService` loads session history + `AiContext` (built by `AiContextService` from persona, AI settings, goals, recent entries; `SystemPromptBuilder` renders it) and delegates to `OrchestratorService`. Streaming is via `SseEmitter` with a status callback (`classifying`, `thinking`); there are parallel sync (`processMessage`) and streaming (`processMessageStream`) paths that must be kept in step.
- `IntentClassifier` maps a message to a `TaskType` (`DAILY_NOTE`, `COACHING`, `GOAL_MANAGEMENT`, `SEARCH`); `TaskHandlerRegistry` resolves the `TaskHandler` in `handler/`.
- **Propose/confirm flow**: mutating handlers (daily note, goal management) don't write directly. They return an `ActionProposal` stored on the assistant `ChatMessage` (`actionType`, `actionPayload` map, `actionStatus` PROPOSED/EXECUTED/REJECTED). On the next user message, if the latest assistant message is PROPOSED, the orchestrator classifies the reply as CONFIRM / REJECT / EDIT; CONFIRM calls `executeProposal`, which turns the untyped payload map into `Create*/Update*Request` records and calls the regular services. Adding a new mutating action means updating the handler, the payload parsing in `OrchestratorService`, and `TaskType`.
- Three `ChatClient` beans in `AiConfig` (`orchestratorClient` cheap/low-temp for classification, `reasoningClient` gemini-pro for coaching, `executionClient` for generation), selected with `@Qualifier`; models configured under `app.ai.models`. `@EnableAsync` is on `AiConfig`.

**Auth**: `JwtAuthenticationFilter` + `SecurityConfig`; short-lived access token (15 min) plus DB-backed refresh tokens (`RefreshToken*`). All endpoints except `/api/auth/**` require Bearer.

**Caching**: Redis (`RedisCacheConfig`, `LoggingCacheErrorHandler` — cache failures are logged, not fatal). Dashboard results are cached under keys `dashboard::<userId>:*`; mutations that affect it must call `CacheEvictionService.evictDashboard(userId)` (no-ops when Redis is absent).

**Errors**: `common/GlobalExceptionHandler` maps `ResourceNotFoundException`, `ConflictException`, etc. to HTTP responses.

## Database

Schema is owned by Flyway (`src/main/resources/db/migration/V*__*.sql`, currently V1–V11); Hibernate runs with `ddl-auto: validate`, so every entity change needs a new migration. Don't edit applied migrations.

## Testing

Tests are `@SpringBootTest` + `@AutoConfigureMockMvc` + `@DirtiesContext(AFTER_EACH_TEST_METHOD)`; each registers a user via `/api/auth/register` and uses the returned token. `src/test/resources/application.yaml` switches to in-memory H2 (PostgreSQL mode), `ddl-auto: create-drop`, Flyway disabled, simple cache — so migration SQL is **not** exercised by tests; verify Postgres-specific SQL manually.

## Notes

- `README.md` is partly stale: it still documents `insight/` and `value/` packages and `/api/insights` endpoints that no longer exist (values removed in V10; chat replaced insights).
