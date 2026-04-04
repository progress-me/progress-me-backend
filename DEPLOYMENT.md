# Deployment

## Prerequisites

- Docker and Docker Compose installed on the production server
- Access to GitHub Container Registry (GHCR)

## Authenticate with GHCR

Create a GitHub Personal Access Token (PAT) with `read:packages` scope, then log in:

```bash
echo $GHCR_TOKEN | docker login ghcr.io -u <github-username> --password-stdin
```

## Docker Compose

Create a `docker-compose.yml` on your server:

```yaml
services:
  app:
    image: ghcr.io/progress-me/progress-me-backend:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/progress_analyzer
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: host.docker.internal
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_API_KEY: ${GOOGLE_API_KEY}
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:17
    environment:
      POSTGRES_DB: progress_analyzer
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  pgdata:
```

## Environment Variables

Create a `.env` file alongside `docker-compose.yml`:

```env
DB_USERNAME=app
DB_PASSWORD=<strong-password>
REDIS_PASSWORD=<strong-password>
JWT_SECRET=<base64-encoded-secret>
GOOGLE_API_KEY=<your-google-api-key>
```

## Run

```bash
docker compose up -d
```

## Update to Latest Image

```bash
docker compose pull app
docker compose up -d app
```
