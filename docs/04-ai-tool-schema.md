# AI tool schema — OpenRouter gateway, allowlisted tools only

## Provider abstraction (`ai` module)
```java
public interface AiProvider {
  ChatResult chat(ChatRequest req);          // req carries system prompt, history, tools
  VisionResult interpret(VisionRequest req);
}
final class OpenRouterProvider implements AiProvider { ... } // only impl; model IDs from env
```
- Env: `OPENROUTER_API_KEY` (server only), `AI_MODEL_CHAT` (default `meta-llama/llama-3.1-8b-instruct:free` → override in prod, e.g. `anthropic/claude-3.5-sonnet`), `AI_MODEL_VISION`, `AI_MODEL_STRUCTURED`, `AI_TIMEOUT_MS=25000`, `AI_MAX_TOKENS=1200`.
- Never call OpenRouter from Android. Android calls `/ai/chat`; backend injects minimal context fetched via services (never dumps whole DB into prompt).
- Cost control: per-user monthly quotas (Redis), per-request token caps, dedupe identical prompts (10s window), fallback model on 5xx/timeout, structured logging of `tokens/cost/latency/tool`.

## System instructions (abridged, enforced server-side)
“You are MealWise AI. PostgreSQL/backend is truth for accounts/inventory/prices/orders/payments/subscriptions/nutrition. Never invent tool results, prices, ETAs, ratings, nutrition numbers, or order states. Call tools for every factual claim. If a tool is unavailable, say so and offer the lawful fallback (handoff, manual entry). General wellness only; no medical diagnosis. Estimates are labelled estimates.”

## Allowlisted tools (typed JSON Schema; LLM cannot run SQL/shell)
| Tool | Args | Truth source |
|---|---|---|
| `getUserProfile` / `getPreferences` | `{}` | users module |
| `getKitchenInventory` | `{includeExpired:false}` | inventory snapshot |
| `getMealHistory` | `{days:14}` | history/plans (feeds no-repeat) |
| `searchMeals` | `{query, diet?, maxMinutes?, budgetPaise?, limit}` | meal search service |
| `getMealDetails` | `{mealId, servings}` | scaled recipe (deterministic) |
| `calculateRecipeQuantities` | `{mealId, servings}` | `RecipeScalingService` |
| `calculateNutrition` | `{mealId, servings}` | `NutritionCalculationService` |
| `calculateMealCost` | `{mealId, servings}` | PriceIntel (only verified quotes; else `unavailable`) |
| `getAvailability` | `{mealId, servings}` | inventory diff engine |
| `createShoppingList` / `addShoppingItems` | `{items[]}` | shopping module |
| `getProviderQuotes` | `{items[]}` | commerce port (handoff-only → returns `unavailable` + handoff URL) |
| `createCart` / `getOrderStatus` | `{...}` | orders (states only) |
| `markMealEaten` / `updateInventory` | `{mealId, servings}` | history + txn |
| `getSubscriptionEntitlements` | `{}` | entitlements service |
| `planMeal` | `{mealId, slot, date, servings}` | planner |

Unknown tools are rejected; args validated with Bean Validation; output validated (e.g. nutrition numbers must equal service result within epsilon or the response is discarded and retried once, then fallback text).

## Example flows
- “What should I eat tonight?” → `getPreferences` + `getMealHistory(7d)` + `getKitchenInventory` → `searchMeals` (filtered) → `getAvailability` top-3 → reply with 1 hero + 2 alternates + reasons (“no paneer repeat — you ate it yesterday”) + `plan_preview` action.
- “Under ₹100, high-protein, <15 min” → same + `calculateNutrition` + `calculateMealCost` (indicative only if no live quotes; UI labels it).
- “Scale to 5” → `calculateRecipeQuantities` (authoritative) → AI narrates.
- Image of fridge → `vision(identify_ingredients)` → suggestion chips with confidence → user confirms → `updateInventory` only on confirm.

## Safety
Timeouts/retries (1 retry, jitter), per-IP + per-user rate limits, prompt-injection guard (external content tagged `<untrusted>` and never treated as instructions), PII minimization (no password/token/payment payload in prompts), full `ai_tool_calls` audit, abuse scoring (block on exfiltration patterns).
