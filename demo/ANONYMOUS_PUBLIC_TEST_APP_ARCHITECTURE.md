# Aura Public Test App Deployment Architecture

## Goal
Aura will be deployed as a simple public test web app that anyone can open in a browser without creating an account.

The deployment should:
- run outside your laptop
- stay simple enough for a test launch
- keep the current Spring Boot monolith approach
- use Neon PostgreSQL for persistent storage
- use Gemini API for hosted AI
- keep Redis off initially to reduce setup complexity

## Final Deployment Plan

```text
Browser
  ->
Render public URL / custom domain
  ->
Spring Boot app on Render
  ->
Neon PostgreSQL

Spring Boot app on Render
  ->
Gemini API
```

Redis is intentionally disabled for the first launch.

## Why This Architecture

This version is the best fit for a public test app because:
- it does not depend on your laptop being online
- it is much simpler than a full production architecture
- Render can host the Spring Boot app on a free tier
- Neon is a clean hosted Postgres option for this backend
- Gemini gives a practical hosted AI option with a free tier
- Redis can be added later only if traffic or AI cost starts to justify it

## Main Components

### 1. Browser
Users access Aura from any browser on desktop or mobile.

Responsibilities:
- load the frontend
- send requests to the backend
- keep the anonymous session cookie
- render extracted transactions, saved entries, reports, and strategy output

Important note:
- the browser is not the source of truth
- important data lives in the backend database

### 2. Render Deployment
Render hosts the Spring Boot application.

Responsibilities:
- build and run the Spring Boot app
- provide environment variables
- expose the application through a public URL

Why Render:
- easier than managing your own server
- free tier is enough for a public test launch
- good fit for a Java monolith

### 3. Spring Boot Monolith
The existing Spring Boot application remains the center of the system.

Responsibilities:
- serve the frontend
- expose REST endpoints
- manage anonymous sessions
- validate requests
- orchestrate transaction extraction, confirmation, analysis, and strategy flows
- store and retrieve session-scoped data
- call Gemini through the backend

Why keep the monolith:
- your codebase already follows this structure
- it is easier to deploy and debug
- splitting services now would add unnecessary complexity

### 4. Neon PostgreSQL
Neon PostgreSQL is the source of truth for app data.

Responsibilities:
- store anonymous sessions
- store transactions
- store session-scoped data needed by the app

Why Neon:
- hosted Postgres without needing a larger backend platform
- good fit when Spring Boot already handles the backend logic
- free tier is practical for a test app

### 5. Gemini API
The AI layer uses Gemini as the hosted provider.

Responsibilities:
- transaction extraction from natural language
- spending insight generation
- financial strategy explanation

Why Gemini:
- easier than self-hosting Ollama
- does not depend on your own machine
- better fit for a free-tier hosted test app than running local model infrastructure

## What We Are Not Using Initially

### Redis
Redis is kept off for the initial public launch.

Why:
- fewer moving parts
- easier first deployment
- the app can already function without it

Redis can be added later for:
- rate limiting
- AI response caching
- temporary request throttling support

### Self-Hosted Ollama
Ollama is not part of the hosted deployment plan.

Why:
- it adds hosting complexity
- it is harder to scale and operate
- it defeats the goal of not depending on your own machine

## Anonymous User Model

Users do not sign up or log in.

Aura identifies each browser using an anonymous session cookie.

Flow:
1. A visitor opens the app.
2. Spring Boot checks for an existing session cookie.
3. If there is no cookie, the backend creates a new anonymous session.
4. The session is stored in PostgreSQL.
5. The browser receives the cookie.
6. All saved data is tied to that anonymous session.

This allows:
- no-account testing
- separate data per visitor
- temporary user isolation

## Request Flow

### First Visit
1. User opens the Render URL or custom domain.
2. Spring Boot checks for an anonymous session cookie.
3. If missing, it creates a new session and stores it in PostgreSQL.
4. The cookie is returned to the browser.

### Quick Log Flow
1. User types a transaction in natural language.
2. Frontend sends the request to Spring Boot.
3. Spring Boot resolves the anonymous session.
4. Spring Boot sends the text to Gemini API.
5. AI returns extracted transaction data.
6. Frontend shows the extracted drafts.
7. User confirms selected entries.
8. Spring Boot saves confirmed entries to PostgreSQL under that session.

### Spending Analysis Flow
1. User requests a spending report.
2. Spring Boot resolves the anonymous session.
3. Spring Boot loads only that session's transactions from PostgreSQL.
4. Spring Boot computes totals and category summaries.
5. Spring Boot calls Gemini for insight text if needed.
6. The result is returned to the browser.

### Financial Strategy Flow
1. User submits date range and optional purchase inputs.
2. Spring Boot resolves the anonymous session.
3. Spring Boot loads that session's transactions.
4. Spring Boot computes financial summaries and projections.
5. Spring Boot calls Gemini for explanation text.
6. The result is returned to the browser.

## Security for the Test Launch

This is not a full production-ready setup, but it still needs basic protection.

Recommended minimum protections:
- HTTPS through Render or custom domain
- environment variables for secrets
- secure cookie settings in deployed environments
- request validation on all endpoints
- do not expose PostgreSQL directly
- do not expose Gemini credentials to the frontend

Optional protections later:
- add Redis-backed rate limiting
- add Cloudflare in front
- add abuse protection if public usage grows

## Environment Strategy

### Local Development
Use local configuration for coding and debugging.

Suggested local setup:
- Spring Boot locally
- local H2 or local Postgres
- current local test configuration for AI if needed

### Hosted Test Environment
Use Render for the web app and Neon for PostgreSQL.

Suggested hosted setup:
- Spring Boot on Render
- Neon PostgreSQL
- Gemini API through backend environment variables
- Redis disabled

## Launch Sequence

The deployment plan should happen in this order:

1. Deploy Spring Boot to Render.
2. Create and connect Neon PostgreSQL.
3. Configure the app to use PostgreSQL in the hosted environment.
4. Configure Gemini API credentials.
5. Keep Redis off initially.
6. Verify anonymous sessions, transaction saving, analysis, and strategy flows.
7. Launch the public test app.

## Final Summary

Aura should now target this hosted test architecture:

```text
Public browser
  ->
Render
  ->
Spring Boot app
  ->
Neon PostgreSQL

Spring Boot app
  ->
Gemini API
```

This keeps the system simple, public, anonymous, and more realistic for a free-tier test launch than a laptop-hosted or self-hosted AI setup.
