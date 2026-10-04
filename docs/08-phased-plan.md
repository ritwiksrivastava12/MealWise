# Phased plan — what this drop delivers and what comes next

## Phase 0 ✅ Blueprint (this drop, in `docs/`)
Requirements, architecture, ERD (Flyway V1 authoritative), REST contract, AI tools, commerce/payments/subs, security/DPDP notes, test strategy, release checklist, integrations-status honesty file.

## Phase 1 ✅ Foundation + vertical slice (this drop, in `backend/` + `android/`)
- Backend boots (`Spring Boot 3.2, Java 21`): security (Argon2id, JWT rotation, Firebase phone verify stub wiring), users/preferences, meals seed (~100), search (trigram), favourites, planner + no-repeat, history, inventory + txns, leftovers, shopping, deterministic scaling/nutrition, commerce port + handoff + sandbox(dev-only), PriceIntel model, cart/orders state machine, Razorpay webhook verify, Play verify + entitlements, AI gateway (OpenRouter + tools + quotas), FCM, audit, admin RBAC, Flyway V1, OpenAPI, Actuator health, unit+API tests.
- Note: small domains are consolidated into one file per domain in this drop (e.g. `planner/Planner.java` holds entities+repos+controller) to keep the vertical slice reviewable; each grows into `web/service/repo/domain/dto` packages before scale-out. Controllers stay thin regardless.
- Android builds (`AGP 8.x, Compose BOM, Hilt, Retrofit, DataStore, Room, Coil, Play Billing 7, FCM`): onboarding (3D-ready Lottie placeholders, reduced-motion aware), auth (email+phone), home hero, search + filters (live), meal detail + availability, cooking mode (steps+timers), kitchen, shopping, AI chat (cards/actions), orders (state + handoff), nutrition, profile/Pro paywall, offline/empty/error states, DI, navigation, accessibility, localization-ready strings (INR/en-IN).
- Infra: `docker-compose` (pg/redis/api), `.env.example`, CI (build+test+migrate-check+secret-scan), `seed_meals.json` validator.

## Phase 2 ⏳ (needs your keys/approvals — see INTEGRATIONS-STATUS)
Swiggy live adapter (after Builders/MCP prod approval), ONDC buyer integration (after registration + spec freeze), Razorpay live + refunds recon, Play Console RTDN wiring, OpenRouter prod model tuning + vision rollout, FCM campaigns, admin console hardening, Play closed-track release.

## Phase 3 (scale)
iOS/Web on same `/api/v1`, modular-service split if load requires, family profiles, advanced analytics, affiliate programs (only verified ones).
