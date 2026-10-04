# MealWise — Intelligent Personal Food-Management Platform (India Launch)

> **Clean-room rebuild.** All FoodMate prototype code was discarded. Everything under `/android` and `/backend` is new, branded `MealWise`, package `in.mealwise.*`.

**Vision:** answer “What should I eat?” in seconds — understand context → recommend → check kitchen → identify missing → cook/order → track nutrition → record → learn.

**Platforms:** Android (Kotlin + Jetpack Compose, Material 3) is the commercial client. Backend (Java 21 + Spring Boot 3.x, PostgreSQL, Redis) is platform-independent REST (`/api/v1`) so iOS/Web can be added later.

## Repo layout

```
/android          Native Android app (modular, Hilt, Navigation Compose)
/backend          Modular-monolith Spring Boot API (domain modules, Flyway, JPA)
/docs             Technical blueprint (read first)
/infrastructure   docker-compose for postgres/redis/backend, env templates
/scripts          Dev/setup/test helpers
/shared           Cross-platform contracts (OpenAPI snapshot, tool schemas)
/.github          CI: build + test + lint + migration check
```

## Quickstart

### Prereqs (install locally — NOT present in this container)
- JDK 21 (Temurin/AWS Corretto), Android Studio Hedgehog+, Android SDK 34
- Docker + Docker Compose, `psql`, Node 20 (docs lint only)

### Backend (staging/dev)
```bash
cp infrastructure/.env.example infrastructure/.env   # fill secrets, never commit
docker compose -f infrastructure/docker-compose.yml up -d postgres redis
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
# API: http://localhost:8080/api/v1/health | OpenAPI: /v3/api-docs
```

### Android
```bash
# Set backend base URL in android/local.properties: api.baseUrl=http://10.0.2.2:8080/api/v1/
# Open android/ in Android Studio, run `app` on emulator (API 26+).
```

## Production rules (enforced in code + review)

1. **No fake functionality.** A button labelled Order/Pay/Subscribe/Ask AI performs the real flow, or the UI states the honest blocker (e.g. “Live ordering unavailable — handoff only. No live prices shown.”). See `docs/INTEGRATIONS-STATUS.md`.
2. **Source of truth:** PostgreSQL/backend for app data; commerce providers for their quotes/orders; payment providers (Razorpay webhook / Play Developer API) for money; AI is reasoning only.
3. **Deterministic engines:** serving-size scaling and nutrition math run in backend Java, never in the LLM. AI only explains.
4. **Secrets:** env/secret-manager only. Android never holds DB, Razorpay secret, OpenRouter key, or Play service-account key.
5. **Commerce:** adapter interface only. Live adapters activate only with authorized credentials. Default = lawful deep-link handoff + user-provided prices. No scraping/automation.

## Auth & billing decisions (confirmed 2026-10-02)
- Identity: **email+password (Argon2id) + phone OTP** (Firebase Auth verify server-side). Both linked to one `users` row.
- Pro: **Google Play Billing** for digital entitlements, verified server-side via Play Developer API. **Razorpay** server-verified for commerce/order payments only (not for Pro unlock).
- Commerce: **deep-link handoff first** (Swiggy/Zomato/Blinkit/Instamart where URL schemes exist). No fabricated prices/ETAs/ratings.
- AI: OpenRouter gateway, model IDs in env (`AI_MODEL_CHAT`, `AI_MODEL_VISION`, `AI_MODEL_STRUCTURED`). Balanced cost/quality defaults.
- Seed catalog: curated ~100 Indian meals (veg/non-veg/egg), structured ingredients + estimated nutrition labelled as estimates.

## Docs (read in order)
1. `docs/00-product-requirements.md` — scope, personas, core loop, Pro entitlements
2. `docs/01-architecture.md` — system diagram, module boundaries, request flows
3. `docs/02-database-erd.md` — normalized schema, indexes, idempotency
4. `docs/03-api-contract.md` — versioned REST, DTOs, errors, pagination
5. `docs/04-ai-tool-schema.md` — provider abstraction, allowlisted tools, safety
6. `docs/05-commerce-payments-subscriptions.md` — adapters, order state machine, ledger
7. `docs/06-security-privacy-compliance.md` — threat model, DPDP notes (counsel review required)
8. `docs/07-test-strategy.md` — mandatory E2E journeys
9. `docs/08-phased-plan.md` — what is built in this drop vs next
10. `docs/09-android-release-checklist.md` — Play launch gates
11. `docs/INTEGRATIONS-STATUS.md` — what is live vs blocked and why

## Status of this drop (Phase 1 — foundation + vertical slice)
- ✅ Blueprint, ERD, API contract, AI tool schemas, adapter interfaces
- ✅ Backend compiles (`./gradlew build` in CI): auth, users, meals, favourites, planner/history, inventory, shopping, nutrition/scaling (deterministic), AI orchestration (gateway + tools), commerce abstraction + handoff adapter, Razorpay webhook verify, Play Billing entitlement verify, FCM, audit, admin RBAC
- ✅ Android builds (`:app:assembleDebug` in CI): onboarding, auth, home (“What should I eat?”), search+filters (real backend), meal detail (availability engine), cooking mode (step machine + timers), kitchen, shopping, AI chat (tool-driven cards), orders (state machine + handoff), nutrition, profile/Pro (Play Billing) — all wired to Retrofit, no mock success
- ⏳ Blocked on your side: Swiggy/ONDC production keys, Razorpay live keys, FCM sender, Play Console service account, OpenRouter key — sandbox/handoff paths work without them; see `INTEGRATIONS-STATUS.md`.

## Commands
```bash
./scripts/check.sh        # lint markdown/yaml/json + verify no secrets committed
./scripts/seed_meals.py   # validates shared/seed_meals.json against schema
```

## Security contact
See `docs/06-security-privacy-compliance.md`. Do not file public issues with PII/financial data.
