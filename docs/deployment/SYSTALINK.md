# Systalink deployment notes

The checked-in Compose file is a local development baseline, not a production-ready public deployment. It binds the frontend to port 3000 and keeps PostgreSQL private to the Compose network.

## Build images before transfer

Run these commands in CI or on a trusted build machine, then build and transfer the Docker images. The Dockerfiles only package the already-built runtime outputs and do not compile on the VPS:

```sh
cd backend && ./gradlew :app:installDist
cd ../frontend && npm ci && KORA_API_URL=http://api:8080 npm run build
cd .. && docker compose build
```

## Before a public deployment

1. Build and verify the frontend and API images in CI, then transfer those images to the VPS; do not compile on the VPS.
2. Use a unique secret manager-generated `POSTGRES_PASSWORD`; never reuse `.env.example`. Set `SESSION_COOKIE_SECURE=true` behind HTTPS.
3. Configure a reverse proxy for TLS, request-size limits, security headers, and forwarding to the frontend. Do not expose PostgreSQL or the API port publicly.
4. Persist the PostgreSQL volume on durable storage. Schedule encrypted backups off-host and test restoration before launch.
5. Configure log retention, monitoring, resource limits, update policy, and a rollback image tag.
6. Smoke-test registration, sign-in, question creation, answer submission, solution acceptance, and the health of all services after deployment.

No domain, certificate, VPS account, or production secret is assumed or configured by this repository. Do not deploy the local placeholder configuration publicly.
