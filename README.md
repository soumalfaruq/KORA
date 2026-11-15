# KORA

KORA is a discussion platform for African developers and the diaspora. This repository starts the MVP with a responsive Next.js interface and a Spring Boot API backed by PostgreSQL. No official logo was present in the repository, so the interface uses a text wordmark rather than an invented symbol.

## Run locally

Requirements: Node.js 22+, Java 17+ (Java 25 is the target where available), Gradle wrapper, and Docker Compose.

1. Copy `.env.example` to `.env` and replace the local-only database password.
2. Build the API distribution with `cd backend && ./gradlew :app:installDist`.
3. Build the frontend image output with `cd frontend && npm ci && KORA_API_URL=http://api:8080 npm run build`.
4. Start the stack with `docker compose up --build` and open <http://localhost:3000>.

The first implemented vertical slice is account registration/sign-in and the discussion forum: search and filter questions, create a question, answer it, and accept a solution as its author. PostgreSQL data is persistent in the Compose volume.

## Development and checks

- Frontend: `cd frontend && npm ci && npm run lint && npm run build`
- Backend: `cd backend && ./gradlew test :app:installDist`
- Local services: `docker compose up --build`

See [docs/PROGRESS.md](docs/PROGRESS.md) for implemented scope and outstanding MVP work. Demo records are not seeded; discussions are created by members.
