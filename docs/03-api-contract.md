# API contract — `/api/v1` (OpenAPI snapshot in `shared/openapi.yaml`; this doc is human-readable)

## Conventions
- Auth: `Authorization: Bearer <accessJwt>` (15m). Refresh: `POST /auth/refresh {refreshToken}` → rotation (old revoked). Phone: Firebase ID token verified server-side then linked.
- Versioning: URL `/api/v1`; breaking → `/api/v2`. Pagination: `?page&size&sort` → `{content, page, size, totalElements, totalPages}`. Errors: `{code, message, correlationId, fieldErrors?}` — never stack traces/SQL. `X-Correlation-ID` echoed.
- INR: amounts as integer paise (`amountPaise`) + `currency:"INR"`; display formats client-side with `hi-IN`/`en-IN`.

## Auth & identity
- `POST /auth/register {email?, phoneE164?, password?, firebaseIdToken?, displayName}` → 201 `{userId}` (at least one verified identity required; password min 10 chars, Argon2id server-side).
- `POST /auth/login {emailOrPhone, password}` → `{accessToken, refreshToken, expiresIn}` + 429 on brute force (Redis bucket).
- `POST /auth/phone/verify {firebaseIdToken}` → link/creates identity.
- `POST /auth/refresh|logout|delete-account|password/forgot|password/reset` — reset via signed single-use token emailed (no enumeration: always 202).

## Users / preferences
- `GET/PATCH /users/me`, `GET/PUT /users/me/preferences {diet, dislikedFoods[], allergies[], goals{proteinG, kcalTarget, dietMode}, budgetTier, maxCookMinutes, householdSize, cookingSkill}`.

## Meals / favourites / planner / history
- `GET /meals/search?q& diet& category& maxMinutes& maxCostPaise& page&size` → trigram+tsvector ranked, filters pushed to SQL.
- `GET /meals/{id}?servings=N` → scaled ingredients + nutrition (deterministic) + `availability` summary if `?includeAvailability=true`.
- `POST /meals/{id}/availability {servings, useLeftovers:true}` → `{available[], low[], missing[]}` with base-unit math.
- `POST/DELETE /users/me/favourites/{mealId}`, `GET /users/me/favourites`.
- `POST /planner {plannedFor, slot, mealId, servings, overrideReason?}` / `GET /planner?from&to` / `PATCH /planner/{id} {status, mealId?, servings?}`.
- `POST /history {mealId, servings, eatenAt?, source}` / `GET /history?limit`.
- `POST /recommendations {slot, servings?, maxMinutes?, budgetPaise?, allowRepeat?}` → `{meal, reason, availability, nutrition}` honouring no-repeat windows.

## Kitchen / shopping
- `GET/POST /inventory`, `PATCH /inventory/{id} {qty, unit, expiresOn}`, `POST /inventory/{id}/consume {qty}`, `GET /inventory/low-expiring`.
- `POST /leftovers {mealId, servings, storedAt}`, `POST /leftovers/{id}/consume|discard`.
- `GET/POST /shopping-lists`, `POST /shopping-lists/items {ingredientId, qty, unit}`, `PATCH .../items/{id}`, `POST /meals/{id}/shopping-diff?servings=N {listId?}` (missing → list).

## Nutrition / scaling (pure functions, tested)
- `POST /nutrition/calculate {mealId, servings}` and `POST /recipes/scale {mealId, fromServings, toServings}` → exact decimal math, kitchen-friendly display rounding only.

## Commerce / orders / payments
- `GET /commerce/providers` → `{name, capabilities[], enabled, handoffSupported}` (swiggy/ondc `enabled:false` until authorized).
- `POST /commerce/handoff {provider, listId?|mealId?}` → `{handoffUrl, disclaimer}` — the only live commerce path at launch.
- `POST /commerce/quotes` → 501 with `PROVIDER_NOT_ENABLED` unless sandbox explicitly enabled (never fake live data).
- `POST /carts`, `POST /orders {cartId, idempotencyKey, provider}` → state machine; `GET /orders/{id}`, `POST /orders/{id}/cancel`.
- `POST /payments/razorpay/webhook` (signature-verified, idempotent), `GET /payments/{id}` (server-fetched status only).

## Subscriptions (Play Billing)
- `GET /subscriptions/plans` (free/pro_monthly/pro_yearly, INR display), `POST /subscriptions/verify {purchaseToken, productId}` → server calls Play Developer API → activates + materializes entitlements; `GET /subscriptions/entitlements`; `POST /subscriptions/cancel` (instructs Play-side cancel + grace handling via RTDN webhook `POST /subscriptions/play-rtdn`).

## AI
- `POST /ai/chat {conversationId?, message, contextOptIn=true, servingsHint?}` → `{reply, actions[]:{type: meal_card|ingredient_diff|shopping_list|handoff|plan_preview, payload}, usage{tokens, quotaRemaining}}` — 429 on quota, 503 with safe fallback on provider timeout.
- `POST /ai/vision {imageUrl|uploadId, intent: identify_ingredients|read_bill|identify_dish}` → suggestions + confidence, never authoritative nutrition.
- Quotas: free 30/mo, pro 1000/mo (Redis + PG). Admin: `GET /admin/ai/usage`.

## Notifications / admin / system
- `POST /devices/fcm-token`, `GET /notifications/preferences`, `PATCH ...`, `GET /health`, `GET /admin/*` (ROLE_ADMIN only).
