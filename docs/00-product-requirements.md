# MealWise product requirements — single source of product truth.
# Any behaviour change must update this file + API contract + tests.

## 1. Personas (India launch)
- Hostel/student, bachelor, young professional, fitness-conscious, household (2-6 servings).

## 2. Core loop (must feel effortless)
NEED → CONTEXT → RECOMMEND → CHECK KITCHEN → MISSING → COOK/ORDER DECISION → SHOP/ORDER → COOK-GUIDED or ORDER-TRACKED → NUTRITION → RECORD → INVENTORY UPDATE → LEARN.

All modules must read/write shared context via backend services, not isolated screens.

## 3. Must-have journeys (E2E tested)
1. Install → onboarding → register (email or phone) → preferences → home → search “paneer” → open → favourite → availability check → shopping list for missing → cook (scaled) → mark eaten → inventory decremented → history → next-day recommendation avoids repeat.
2. Fitness: set high-protein goal → plan dinner → scaled nutrition shown (deterministic) → AI explains, does not compute.
3. Commerce: missing items → shopping list → provider options (handoff only at launch, no live prices) → deep-link out with attribution → manual “mark purchased” → inventory += verified user event.
4. Pro: free user hits AI quota → paywall → Play Billing purchase → backend verifies via Play Developer API → entitlements unlock → cancel → grace/expiry → entitlements downgrade.

## 4. No-repeat logic (configurable, not hardcoded)
- `recommendation.avoid_repeat_meal_within_days` (default 3), `avoid_repeat_primary_ingredient_within_days` (default 2), both user-overridable per plan request.
- Planner service queries `meal_history` + `meal_plans` windows; user can explicitly override (“I want paneer again”) — override is logged.

## 5. Entitlements (single source: SubscriptionEntitlementService)
| Capability | Free | Pro |
|---|---|---|
| Meal search/browse/favourites/history | yes | yes |
| Daily planner + basic recommendations | yes (1 day) | yes (30 days + family profiles) |
| Kitchen inventory + shopping lists | yes | yes |
| Cooking mode | yes | yes |
| MealWise AI messages/month | 30 (rate-limited) | 1000 + priority + vision |
| Advanced nutrition (per-day targets, analytics) | basic per-meal only | full |
| Extended history/analytics | 30 days | 365 days |
| PriceIntel live quotes | handoff only | handoff only (no paywalled safety) |
Basic safety/usability is never paywalled.

## 6. Non-goals for launch
- iOS/Web clients (API must stay platform-independent).
- In-app live checkout with Swiggy/ONDC (handoff only until authorized).
- Medical nutrition claims. Copy must say “general wellness” + “estimates”.

## 7. Copy rules
- Prices always show source + timestamp + freshness. Never show indicative as live.
- Nutrition from USDA/IFCT-derived or computed data labelled `estimated` unless lab-verified.
- Leaving MealWise: “You’re opening <provider>. MealWise doesn’t control prices/availability there.”
