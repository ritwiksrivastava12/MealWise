# Test strategy (mandatory — CI fails without these)

## Backend (`./gradlew test`, JaCoCo ≥80% on domain services)
- Unit: `RecipeScalingService` (2→4 servings exact decimals, kitchen-friendly display), `NutritionCalculationService` (per-100g math, estimate labelling), `NoRepeatPlanner` (paneer-eaten-yesterday is demoted, override respected), `OrderStateMachine` (illegal transition throws), `PriceNormalizer` (200g ≠ 400g), `EntitlementService` (matrix test free vs pro).
- API (MockMvc + Testcontainers PG/Redis): register→login→refresh→logout; search relevance; favourites add/remove; plan→eat→history→recommend avoids repeat; inventory→availability→shopping-diff→consume; AI chat executes real tools (WireMock OpenRouter); Razorpay webhook HMAC + idempotent replay; Play verify mocked + RTDN grace→expired.
- Contract: `shared/openapi.yaml` lint + consumer test (Android Retrofit DTOs deserialize sample payloads); Flyway migrate + validate on PG 15.
- Security: authZ matrix (user cannot hit `/admin`), rate-limit bucket, JWT expiry, PII leak scan on error payloads.

## Android (`./gradlew :app:testDebugUnitTest`, emulator `connectedCheck` nightly)
- ViewModel tests ( Turbine ): search debounce + filters call repo once; availability partitions; cooking step machine + timer pause/resume + scale refresh; AI actions render cards; ProGate blocks/allows per entitlements.
- UI (Compose Test): onboarding → home hero answers “What should I eat?” <2 taps; offline banner (airplane) never claims order success; handoff interstitial shows provider name.
- E2E (staging, sandbox keys): full loop in `docs/00` journey 1–4; order handoff → manual purchased → inventory; Pro purchase (Play test track) → entitlement → cancel → downgrade.

## Performance/compat gates
Cold start <2.5s on Moto G (API 26), search p95 <800ms (server), AI p95 <12s with fallback text, APK <40 MB (R8 + WebP + dynamic 3D on-demand), accessibility (TalkBack traversal, 48dp targets, contrast AA).
