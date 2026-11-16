# KORA implementation status

## Initial audit

The repository contained only a placeholder README. There was no application code, test tooling, database configuration, or logo. The implementation therefore starts from a clean scaffold; no existing product behavior was available to preserve.

## Current scope

- [x] Next.js App Router frontend with strict TypeScript and responsive KORA styling.
- [x] Spring Boot API and PostgreSQL migration for users, questions, answers, and tags.
- [x] Forum vertical slice: accounts, searchable/paginated questions, answers, and author-only solution acceptance.
- [x] Local Docker Compose configuration and focused tests.
- [ ] P0 forum work: verify question editing end to end; then question deletion, answer editing/deletion, comments, and reactions.
- [x] Systalink Web deployment: the public frontend responds with HTTP 200.
- [ ] Systalink API and production validation: both the API domain and the Web proxy return HTTP 404 `text/html` for `/api/health`. The Web service's `KORA_API_URL` points to the API domain, and the API's Docker root is `backend/`; investigate Systalink routing/startup, then verify database connectivity and end-to-end flows.
- [ ] Complete community and project workflows.
- [ ] Notifications, moderation queue, administration, full i18n, and broader profile/preferences flows.
- [ ] Production TLS, backup automation, rate limiting, and operational hardening.

## Latest lot in progress: question editing

- The frontend and API path are implemented; the API enforces question ownership, validates and normalizes tags, and updates the existing `updated_at` field.
- Backend service tests cover author access, denied edits, normalized/invalid tags, missing questions, and preservation of resolved status and accepted answers.
- `git diff --check`, `cd backend && ./gradlew test`, `cd frontend && npm run lint`, and the frontend production build with `KORA_API_URL=http://api:8080` passed.
- Live HTTP/PostgreSQL persistence and browser-flow verification have not been run, so question editing is not yet counted as operational.
- A local `docker compose ps` check could not reach the selected Docker Desktop Linux engine. No services were started, and COLIMA was not accessed.
- The user-supplied logo is integrated into the header, dark hero, footer, and site icon; deployment of this UI change is pending.

## Assumptions and limitations

- Java 17 is used as the available local toolchain; the code uses Spring Boot 4 and can be moved to Java 25 when that runtime is available.
- The user-supplied logo is included in the repository as color and white assets plus the application icon.
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
- `docs/deployment/SYSTALINK-QUICKSTART.md` documents the verified public product claims separately from dashboard-only settings. PostgreSQL managed is confirmed; its password must remain a dashboard environment secret, and the JDBC URL uses `sslmode=require`.
- The frontend is deployed and responds with HTTP 200. The Systalink dashboard confirms `KORA_API_URL` points to the API service, whose Docker root is `backend/`; database URL, username, password variable, and secure-cookie setting are configured. Direct API and frontend-proxied `/api/health` checks both return HTTP 404 `text/html`, not the expected JSON. Systalink's events/logs pages returned 503 and its serverless console rejected a read-only health-check command as unauthorized, so service startup/routing cannot be diagnosed or changed from the current dashboard session. No production end-to-end forum workflow has been verified, and test registration cannot proceed.
- Docker Desktop remains unavailable locally; this does not block the remote Systalink deployments. COLIMA was not accessed.
