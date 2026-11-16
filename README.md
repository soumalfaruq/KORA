# KORA

KORA is a discussion platform for African developers and the diaspora. This repository starts the MVP with a responsive Next.js interface and a Spring Boot API backed by PostgreSQL. No official logo was present in the repository, so the interface uses a text wordmark rather than an invented symbol.

## Run locally

To run the local stack, Docker Compose is required. Native checks need Node.js 22+, Java 17+, and the Gradle wrapper.

1. Copy `.env.example` to `.env` and replace the local-only database password.
2. Start the stack with `docker compose up --build` and open <http://localhost:3000>. The Dockerfiles build the API and frontend from source.

The first implemented vertical slice is account registration/sign-in and the discussion forum: search and filter questions, create a question, answer it, and accept a solution as its author. PostgreSQL data is persistent in the Compose volume.

## Development and checks

- Frontend: `cd frontend && npm ci && npm run lint && npm run build`
- Backend: `cd backend && ./gradlew test :app:installDist`
- Local services: `docker compose up --build`

See [docs/PROGRESS.md](docs/PROGRESS.md) for implemented scope and outstanding MVP work. Demo records are not seeded; discussions are created by members.
