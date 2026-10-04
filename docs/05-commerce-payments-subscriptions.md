# Commerce, payments, subscriptions — honest-by-design

## Commerce port (core never knows provider names)
```java
public interface CommerceProvider {
  String name(); ProviderCapabilities capabilities();   // SEARCH, QUOTE, CART, CHECKOUT, STATUS, CANCEL, HANDOFF
  HandoffResult handoff(HandoffRequest r);              // deep-link URL + disclaimer — always allowed
  QuoteResult quote(QuoteRequest r);                    // live only if provider enabled+authorized
}
```
- `HandoffProvider` (default, enabled): builds Swiggy/Zomato/Blinkit search URLs from ingredient/meal names, e.g. `https://www.swiggy.com/search?query=paneer` — shows “You’re leaving MealWise” interstitial. No price/ETA claims.
- `SandboxCommerceProvider` (`mealwise.commerce.sandbox.enabled=true` in dev only): returns clearly-labelled `source_type=test` quotes for E2E tests; blocked in prod by config guard.
- `SwiggyAdapter`, `OndcBuyerAdapter`: separate classes, `@ConditionalOnProperty(...enabled=true)`, default OFF. They activate only after you supply official credentials + registration. ONDC implements search→select→init→confirm→status→cancel + callback signature verify per current spec (do not invent fields — implement against official docs at integration time).

## PriceIntel
Every price row carries `provider, normalized_item, price_paise, delivery_fee_paise, currency, unit, source_type[live|affiliate|user|indicative|cached|test], fetched_at, valid_until, confidence`. API `GET /commerce/quotes` returns `freshness: live|stale|unavailable`. Android `PriceRow` composable renders source+time (“Swiggy · 2 min ago · live”) or “No live price — handoff only”. Comparing “Amul Paneer 200g” variants: normalize via `ingredient_aliases` + size-aware match; different sizes never merged (show per-100g alongside).

## Orders (real state machine)
States: CREATED → QUOTE_PENDING → CONFIRMED → PAYMENT_PENDING → PAID → ACCEPTED → PREPARING → DISPATCHED → DELIVERED, plus CANCEL_REQUESTED → CANCELLED, FAILED, REFUND_PENDING → REFUNDED. `OrderService.transition(id, to, actor)` validates against allowed map, bumps `version` (optimistic lock), appends `state_history`, writes audit. Idempotency: `orders.idempotency_key UNIQUE` — retry with same key returns existing order. At launch most orders are `HANDOFF` kind (`provider=handoff`, state `CONFIRMED→DISPATCHED` not tracked; UI says “Completed in provider app — mark purchased manually”).

## Payments
- **Razorpay (commerce only):** Android uses Razorpay Checkout SDK with `order_id` created server-side (`POST /payments/razorpay/orders`). Success callback is untrusted — backend verifies via `payments/razorpay/webhook` (HMAC-SHA256 of body with `RAZORPAY_WEBHOOK_SECRET`) + server-side fetch of payment status; only then `orders→PAID`. Refunds via Dashboard/API with `ledger_entries(kind=refund)`.
- **Never:** trust client `payment.success`, store card data, or put Razorpay key-secret in the app (only `RAZORPAY_KEY_ID` public key ships in app config).

## Subscriptions / Pro (Play Billing)
- Android uses Play Billing Library 7 (`BillingClient.queryProductDetails/launchBillingFlow`). Purchase token is sent to `POST /subscriptions/verify`; backend calls Play Developer API (`purchases.subscriptionsv2.get`) with service-account credentials to confirm `acknowledged+active`; only then writes `subscriptions(status=active)` + materializes `entitlements`. `EntitlementFilter` (Spring interceptor) + Android `ProGate` both read the same capability list — no scattered `if (pro)` checks.
- Renewal/cancel/expiry via Real-time Developer Notifications (`POST /subscriptions/play-rtdn`, JWT-verified) → `grace → expired` transitions + FCM to user. Refunds mirror to `ledger_entries`.
- Razorpay is NOT used for Pro unlock (Play policy). Free-tier safety features are never gated.

## Ledger
`ledger_entries` per money event: gross, provider_fee, payment_fee, commission, tax, refund, net. Display prices ≠ accounting truth; reconciliation job nightly (`PaymentReconciliationJob`) flags mismatches to admin.
