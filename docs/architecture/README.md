# Architecture

- `frontend/`: Next.js App Router, React, strict TypeScript, and responsive CSS. The server-side rewrite at `/api/*` forwards same-origin browser requests to Spring Boot; the browser never needs direct API credentials.
- `backend/app/`: Spring Boot 4.1 API, split into `auth`, `forum`, and `api` packages. JPA persists domain entities; Flyway owns the PostgreSQL schema.
- `docker-compose.yml`: local PostgreSQL, API, and standalone Next.js services. PostgreSQL data is held in a named volume.

Registration and login create database-backed member accounts and Spring HTTP sessions. Mutations require the session and Spring Security CSRF token. Public reads use DTOs so JPA relationships and password hashes are not serialized.

The official logo was not in the original repository. The frontend uses a simple text wordmark until the approved asset is supplied.
