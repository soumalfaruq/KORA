# KORA implementation status

## Initial audit

The repository contained only a placeholder README. There was no application code, test tooling, database configuration, or logo. The implementation therefore starts from a clean scaffold; no existing product behavior was available to preserve.

## Current scope

- [x] Next.js App Router frontend with strict TypeScript and responsive KORA styling.
- [x] Spring Boot API and PostgreSQL migration for users, questions, answers, and tags.
- [x] Forum vertical slice: accounts, searchable/paginated questions, answers, and author-only solution acceptance.
- [x] Local Docker Compose configuration and focused tests.
- [ ] P0 forum work: verify question editing end to end; then question deletion, answer editing/deletion, comments, and reactions.
- [ ] Systalink deployment: PostgreSQL configured; KORA API and KORA Web build/runtime settings prepared; HTTPS URLs and live testing pending.
- [ ] Complete community and project workflows.
- [ ] Notifications, moderation queue, administration, full i18n, and broader profile/preferences flows.
- [ ] Production TLS, backup automation, rate limiting, and operational hardening.

## Latest lot in progress: question editing

- The frontend and API path are implemented; the API enforces question ownership, validates and normalizes tags, and updates the existing `updated_at` field.
- Backend service tests cover author access, denied edits, normalized/invalid tags, missing questions, and preservation of resolved status and accepted answers.
- `git diff --check`, `cd backend && ./gradlew test`, `cd frontend && npm run lint`, and the frontend production build with `KORA_API_URL=http://api:8080` passed.
- Live HTTP/PostgreSQL persistence and browser-flow verification have not been run, so question editing is not yet counted as operational.
- A local `docker compose ps` check could not reach the selected Docker Desktop Linux engine. No services were started, and COLIMA was not accessed.
- The official logo was supplied in chat; its UI integration is deferred until the P0 forum workflow is verified.

## Assumptions and limitations

- Java 17 is used as the available local toolchain; the code uses Spring Boot 4 and can be moved to Java 25 when that runtime is available.
- The official logo is available from the user but has not yet been added to the repository; the current UI still uses a text wordmark.
- No demonstration accounts or seeded discussions are created. A member must register and create real content.
- Compose is for local development; production secrets, HTTPS termination, backups, and monitoring must be configured by the operator.

## Previously recorded baseline verification

- `cd frontend && npm run lint && KORA_API_URL=http://api:8080 npm run build && npm audit` — passed; no npm advisories reported.
- `cd backend && ./gradlew test :app:installDist` — passed.
- `docker compose --env-file .env.example config --quiet` — passed.
- Built and started the Compose stack; PostgreSQL, API, and frontend health checks passed. Browser smoke test covered account creation, question publication, second-member answer, author acceptance, tag search, and 390px viewport without horizontal overflow.

These are baseline results from the earlier forum lot; they do not verify the new question-edit flow.

## Deployment preparation: Systalink

- Backend and frontend Dockerfiles now build their applications from the Git checkout; local Compose passes the internal API origin as a build argument.
- Spring Boot honors the platform-provided `PORT`, while preserving the local default of 8080. The frontend build now requires `KORA_API_URL` in production and the image no longer fixes `PORT`.
- Docker build contexts exclude local environment files. Compose keeps PostgreSQL and API unexposed and binds the Web port to host loopback for the Caddy VPS fallback.
- `docs/deployment/SYSTALINK-QUICKSTART.md` documents the verified public product claims separately from dashboard-only settings. PostgreSQL managed is now confirmed: host, port, database, user, and password are provided; the JDBC URL format is corrected to `sslmode=require`. KORA API and KORA Web configuration tables are ready for Systalink dashboard entry.
- Docker Desktop was unavailable. Container builds, Compose runtime, and Systalink dashboard settings were not tested; no subscription, resource, or remote deployment was created, and COLIMA was not accessed.
