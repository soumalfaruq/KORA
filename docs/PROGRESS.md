# KORA implementation status

## Initial audit

The repository contained only a placeholder README. There was no application code, test tooling, database configuration, or logo. The implementation therefore starts from a clean scaffold; no existing product behavior was available to preserve.

## Current scope

- [x] Next.js App Router frontend with strict TypeScript and responsive KORA styling.
- [x] Spring Boot API and PostgreSQL migration for users, questions, answers, and tags.
- [x] Forum vertical slice: accounts, searchable/paginated questions, answers, and author-only solution acceptance.
- [x] Local Docker Compose configuration and focused tests.
- [ ] Complete community and project workflows.
- [ ] Notifications, moderation queue, administration, full i18n, and broader profile/preferences flows.
- [ ] Production TLS, backup automation, rate limiting, and operational hardening.

## Assumptions and limitations

- Java 17 is used as the available local toolchain; the code uses Spring Boot 4 and can be moved to Java 25 when that runtime is available.
- The official logo is absent. A text wordmark is used until the supplied official asset can be added.
- No demonstration accounts or seeded discussions are created. A member must register and create real content.
- Compose is for local development; production secrets, HTTPS termination, backups, and monitoring must be configured by the operator.

## Verification

- `cd frontend && npm run lint && KORA_API_URL=http://api:8080 npm run build && npm audit` — passed; no npm advisories reported.
- `cd backend && ./gradlew test :app:installDist` — passed.
- `docker compose --env-file .env.example config --quiet` — passed.
- Built and started the Compose stack; PostgreSQL, API, and frontend health checks passed. Browser smoke test covered account creation, question publication, second-member answer, author acceptance, tag search, and 390px viewport without horizontal overflow.
