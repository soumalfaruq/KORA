# Test plan

## Automated checks

- Frontend: `cd frontend && npm ci && npm run lint && npm run build`
- Backend: `cd backend && ./gradlew test`
- Dependency audit: `cd frontend && npm audit`

The focused backend tests cover the solution-acceptance permission boundary and the successful author workflow. In the local Compose stack, the manual two-member flow (register, publish, answer, accept), tag search, and 390px mobile overflow check were exercised successfully. Compose health checks reported PostgreSQL, API, and frontend healthy.

## Manual smoke test

1. Start `docker compose up --build` and open `http://localhost:3000`.
2. Register and sign in with a new member account.
3. Publish a question with tags; confirm it appears in search and status filters.
4. Sign out and create a second member; open the question and submit an answer.
5. Sign back in as the question author and accept that answer.
6. Confirm the question is marked resolved and cannot receive further answers.
7. Attempt to accept the answer as the second member and confirm the API rejects the request.

Tests requiring a running PostgreSQL instance and cross-device deployment have not been claimed as passed unless run separately.
