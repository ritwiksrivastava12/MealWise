# Architecture — MealWise modular monolith + native Android

## System diagram (logical)
```
Android (Compose, Hilt, Retrofit, DataStore, Room cache, WorkManager, FCM, Play Billing)
   │ HTTPS + JWT (access 15m / refresh 30d rotation)  correlation-id per request
   ▼
API Gateway concerns inside Spring Boot: rate-limit (Redis), auth, validation, audit
   ▼
Domain modules (no cross-import of impls, only via service interfaces + domain events):
 auth/identity · users/profiles/preferences · meals/recipes/ingredients/units
 favourites · planner/history · inventory · shopping · nutrition/scaling
 commerce (port) + adapters (handoff, sandbox, [swiggy|ondc gated]) · priceintel
 cart/orders (state machine) · payments (razorpay verify) · subscriptions/entitlements (play verify)
 ai-orchestration (gateway→OpenRouter, tools, guardrails, cost control) · notifications (FCM)
 analytics · audit · admin (RBAC) · config/flags
   ▼
PostgreSQL (source of truth, Flyway) · Redis (cache/quotas/idempotency) · Async workers (Spring @Async + ShedLock)
   │ out-of-process (server-side only)
   ├── OpenRouter (chat/vision/structured) — never from Android
   ├── Razorpay (orders) — verify webhook signature + fetch payment server-side
   ├── Google Play Developer API — verify Pro purchase token server-side
   ├── FCM — push
   └── Commerce providers — handoff URLs at launch; live adapters behind capability flags
```

## Key decisions
- **Modular monolith**, package `in.mealwise.api.<domain>` with `web/ service/ repo/ domain/ dto/ mapper/`; controllers are thin (validation + auth + DTO map only).
- **Platform-independent REST** `/api/v1`, versioned, OpenAPI at `/v3/api-docs`. No JPA entities leave the server (MapStruct-style manual mappers for control).
- **Deterministic engines are pure Java services** (`RecipeScalingService`, `NutritionCalculationService`) with unit tests; AI layer may call them as tools but never replaces them.
- **AI gateway**: `AiProvider` port → `OpenRouterProvider` now; model IDs from env; per-user quotas in Redis; tool allowlist only; all tool calls logged with latency/tokens/cost.
- **Commerce port** (`CommerceProvider`): `capabilities()`, `quote()`, `handoff()`. Core never branches on provider name. Swiggy/ONDC adapters are separate classes, `@ConditionalOnProperty(mealwise.commerce.<name>.enabled)` — default OFF.
- **Orders**: explicit state machine with transition table + idempotency keys (DB unique). Webhooks reconcile, never trust client.
- **Android**: single-activity + Navigation Compose, feature modules (`:core:*`, `:feature:*`), ViewModel+StateFlow, Repository+Retrofit+Room cache, DataStore for tokens (EncryptedFile/Keystore), Coil for images, Coroutines/Flow throughout. No god Activity/composable.

## Request flows
- Search: Android `MealRepository.search()` → `GET /meals/search` → service ( trigram `pg_trgm` + filters) → DTO page. No local fake filtering.
- Availability: `POST /meals/{id}/availability?servings=N` → loads recipe + inventory snapshot → partitions available/low/missing (unit-normalized to base g/ml/pc).
- Cook: `GET /meals/{id}?servings=N` returns scaled quantities server-side; Android `CookingViewModel` is a step machine (timers via coroutines), progress resumable (SavedStateHandle + server `cooking_sessions` optional v2).
- AI: Android sends `POST /ai/chat {message, contextOptIn}` → gateway loads minimal context via backend services → OpenRouter with tools → executes allowlisted tools server-side → returns message + `actions[]` (mealCards, ingredient diffs, deep-links) that Android renders natively.

## Environments
dev (H2 allowed? No — docker postgres always, to avoid dialect drift) · staging (prod-like data, sandbox keys) · prod (managed PG, Redis, secrets manager). Dev builds never point at prod DB (config guard fails fast if `prod` host used with `dev` profile).
