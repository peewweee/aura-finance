# Aura Render Deployment Checklist

## Goal
Deploy Aura as a public test app on Render with:
- Spring Boot
- Neon PostgreSQL
- Gemini API
- Redis disabled initially

## Render App Setup

1. Create a new Web Service on Render for this repository.
2. Set the app profile to `render`.
3. Connect the repository and let Render build the Spring Boot app.

Suggested environment variable:
- `SPRING_PROFILES_ACTIVE=render`

## Neon Database Setup

1. Create a Neon project and database.
2. Copy the connection values.
3. Add them to the Render service environment.

Use either:
- `AURA_DB_URL`
- `AURA_DB_USERNAME`
- `AURA_DB_PASSWORD`

Or the equivalent Postgres variables:
- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

## Gemini API Setup

Set these on the Render service:
- `AURA_AI_GEMINI_API_KEY`

Optional:
- `AURA_AI_GEMINI_BASE_URL`
- `AURA_AI_GEMINI_MODEL_NAME`

Recommended first-pass AI setting:
- `AURA_AI_GEMINI_MODEL_NAME=gemini-2.5-flash`

Only the backend should know the Gemini API key.

## Redis

Leave Redis disabled for the initial launch.

Current default:
- `aura.redis.enabled=false`

Do not add Redis until the deployed app is already working end to end.

## Deployment Verification

After deployment, verify these flows:

1. Open the public Render URL.
2. Confirm the app loads correctly.
3. Submit a quick log entry.
4. Confirm extracted transactions appear.
5. Save selected transactions.
6. Reload and confirm the session still sees its own saved data.
7. Run spending analysis.
8. Run financial strategy.

## Important Notes

- This is a public anonymous test app, not a hardened production system.
- Session data is separated by anonymous cookie-based session IDs.
- PostgreSQL is the source of truth.
- Hosted AI calls happen through the backend, never from the frontend.
- Render free services may sleep when idle, so cold starts are expected.

## Future Additions

Only after the Render launch works:
- add Redis
- enable rate limiting with Redis
- add a custom domain
- add stronger abuse protection if needed
