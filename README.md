# Progress Me Backend

A personal progress tracking and AI-powered insight engine. Users log daily entries (journal, reflections, wins,
struggles), organize them under goals and life categories, and receive AI-generated insights powered by Google Gemini.

## Tech Stack

| Layer      | Technology                                          |
|------------|-----------------------------------------------------|
| Language   | Java 21                                             |
| Framework  | Spring Boot 4.0                                     |
| Database   | PostgreSQL                                          |
| Migrations | Flyway                                              |
| Auth       | JWT (jjwt) + Spring Security, stateless             |
| AI         | Spring AI + Google Gemini (`gemini-3.5-flash-lite`, `gemini-4-argon`) |
| Build      | Gradle (Groovy DSL)                                 |
| Container  | Docker (multi-stage, Eclipse Temurin 21)            |

## Domain Model

```
User 1──1 Persona          (self-description for AI context)
User 1──1 AiSettings       (tone, strictness, focus/ignore areas, language)
User 1──* Category          (life areas: career, health, etc.)
User 1──* UserValue         (personal values, linked to a category)
User 1──* Goal              (active/completed/paused, linked to categories)
User 1──* LogEntry          (journal entries with type, mood, energy)
User 1──* Insight           (AI-generated analysis tied to source entries)

LogEntry *──* Goal           (entry can track multiple goals)
LogEntry *──* UserValue      (tagging entries with personal values)
Goal     *──* Category       (goal spans multiple life areas)
```

## API Endpoints

All endpoints except `/api/auth/**` require a `Bearer` token in the `Authorization` header.

### Auth

| Method | Path                 | Description         |
|--------|----------------------|---------------------|
| POST   | `/api/auth/register` | Register a new user |
| POST   | `/api/auth/login`    | Login, returns JWT  |
| GET    | `/api/me`            | Current user info   |

### Persona & AI Settings

| Method | Path                  | Description                 |
|--------|-----------------------|-----------------------------|
| GET    | `/api/me/persona`     | Get user persona            |
| PUT    | `/api/me/persona`     | Update user persona         |
| GET    | `/api/me/ai-settings` | Get AI behavior settings    |
| PUT    | `/api/me/ai-settings` | Update AI behavior settings |

### Categories

| Method | Path                   | Description         |
|--------|------------------------|---------------------|
| POST   | `/api/categories`      | Create category     |
| GET    | `/api/categories`      | List all categories |
| GET    | `/api/categories/{id}` | Get category by ID  |
| PUT    | `/api/categories/{id}` | Update category     |
| DELETE | `/api/categories/{id}` | Delete category     |

### Values

| Method | Path                             | Description                   |
|--------|----------------------------------|-------------------------------|
| POST   | `/api/categories/{catId}/values` | Create value under a category |
| GET    | `/api/values`                    | List all user values          |
| PUT    | `/api/values/{id}`               | Update a value                |
| DELETE | `/api/values/{id}`               | Delete a value                |

### Goals

| Method | Path                     | Description             |
|--------|--------------------------|-------------------------|
| POST   | `/api/goals`             | Create goal             |
| GET    | `/api/goals`             | List goals (filterable) |
| GET    | `/api/goals/{id}`        | Get goal by ID          |
| PUT    | `/api/goals/{id}`        | Update goal             |
| PUT    | `/api/goals/{id}/status` | Change goal status      |
| DELETE | `/api/goals/{id}`        | Delete goal             |

### Log Entries

| Method | Path                         | Description              |
|--------|------------------------------|--------------------------|
| POST   | `/api/log-entries`           | Create log entry         |
| GET    | `/api/log-entries`           | List entries (paginated) |
| GET    | `/api/log-entries/{id}`      | Get entry by ID          |
| PUT    | `/api/log-entries/{id}`      | Update entry             |
| PUT    | `/api/log-entries/{id}/tags` | Update entry tags        |
| DELETE | `/api/log-entries/{id}`      | Delete entry             |

### Insights (AI)

| Method | Path                    | Description                      |
|--------|-------------------------|----------------------------------|
| GET    | `/api/insights/prompts` | List prebuilt analysis prompts   |
| POST   | `/api/insights/analyze` | Generate AI insight from entries |
| GET    | `/api/insights`         | List past insights (paginated)   |
| GET    | `/api/insights/{id}`    | Get insight by ID                |

### Dashboard

| Method | Path             | Description                                          |
|--------|------------------|------------------------------------------------------|
| GET    | `/api/dashboard` | Aggregated stats: streaks, mood/energy trends, goals |

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 15+
- Google AI API key (for Gemini)

### Local Setup

1. **Create the database:**

   ```bash
   createdb progress_analyzer
   ```

2. **Set environment variables:**

   ```bash
   export GOOGLE_API_KEY=<your-gemini-api-key>
   export JWT_SECRET=<base64-encoded-secret>  # optional, has dev default
   ```

3. **Run the application:**

   ```bash
   ./gradlew bootRun
   ```

   The server starts on `http://localhost:8080`. Flyway runs migrations automatically.

4. **Run tests:**
   ```bash
   ./gradlew test
   ```
   Tests use an in-memory H2 database.

### Docker

```bash
docker build -t progress-me-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/progress_analyzer \
  -e SPRING_DATASOURCE_USERNAME=app \
  -e SPRING_DATASOURCE_PASSWORD=dev \
  -e GOOGLE_API_KEY=<your-key> \
  progress-me-backend
```

## Project Structure

```
src/main/java/com/nhavronskyi/progressmebackend/
  ai/            # AI config, context builder, system prompt, settings
  auth/          # JWT filter, auth service, login/register
  category/      # Life area categories
  common/        # Global exception handler, shared exceptions
  config/        # Security and CORS configuration
  dashboard/     # Aggregated analytics endpoint
  goal/          # Goal tracking (active, completed, paused)
  insight/       # AI-generated insights from log entries
  log/           # Daily log entries (journal, reflections)
  persona/       # User self-description for AI personalization
  user/          # User entity and repository
  value/         # Personal values (linked to categories)

src/main/resources/
  db/migration/  # Flyway SQL migrations (V1-V7)
  db/seed/       # Seed data
  application.yaml
```
