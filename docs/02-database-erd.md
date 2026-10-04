# Database ERD (PostgreSQL 15+, Flyway V1__init.sql is authoritative)

Conventions: UUID PKs (`gen_random_uuid()`), `created_at/updated_at` (timestamptz), `deleted_at` soft-delete where user-facing, `version` optimistic lock on orders/carts/inventory, FKs indexed, `*_idempotency_key` unique.

## Core tables
- `users(id, email CITEXT UNIQUE, phone_e164 UNIQUE, password_hash, auth_provider, status, created_at, ...)` — one row per human; email+phone both nullable but at least one verified. `user_identities(user_id, provider[email|phone_firebase], provider_uid, verified_at)` for “both email+phone” linking.
- `user_profiles(user_id PK/FK, display_name, household_size, cooking_skill[beginner|intermediate|advanced], default_servings)`
- `user_preferences(user_id PK/FK, diet[vegetarian|non_veg|eggetarian], disliked_foods text[], allergies text[], goals jsonb, budget_tier, max_cook_minutes, updated_at)` — diet extensible via CHECK + code enum.
- `sessions(id, user_id, refresh_hash, device_info, expires_at, revoked_at)` — refresh rotation; access JWT stateless, refresh opaque hashed.

## Meals/catalog (curated seed, admin-managed)
- `ingredients(id, name CITEXT UNIQUE, category, base_unit[g|ml|pc], density_g_per_ml, nutrition_per_100g jsonb {kcal,protein,carbs,fat,fibre}, source[label: ifct|usda|vendor|estimate], updated_at)` + `ingredient_aliases(alias CITEXT, ingredient_id)` for “Amul Paneer 200g” normalization (never auto-merge different sizes).
- `meals(id, slug UNIQUE, name, description, diet, categories text[], difficulty, prep_minutes, cook_minutes, servings_default, image_url, tags text[], is_active, search_tsv)` + GIN on `search_tsv`, trigram on `name`.
- `meal_ingredients(meal_id, ingredient_id, qty_per_serving, unit, preparation_note, is_optional)` — structured, never prose-only.
- `recipe_steps(meal_id, step_no, title, instruction, requires_seconds, temperature_c, tips)` — ordered; cooking mode reads this.
- `favourites(user_id, meal_id, created_at, PK(user_id,meal_id))`
- `meal_plans(id, user_id, planned_for date, slot[breakfast|lunch|snack|dinner], meal_id, servings, status[planned|skipped|eaten|replaced], rec_explanation, override_reason, version)`
- `meal_history(id, user_id, meal_id, eaten_at, servings, source[planned|ad_hoc|leftover|ordered], calories_est)` — feeds no-repeat.
- `recommendation_feedback(user_id, meal_id, action[accepted|rejected|overrode], reason, created_at)`.

## Kitchen/shopping
- `inventory_items(id, user_id, ingredient_id, qty, unit, qty_base, status[available|low|expired|exhausted], expires_on, source[manual|purchase|leftover], lot_ref, version)` + `inventory_txns(id, item_id, delta_base, reason[consume|purchase|waste|adjust|cook], ref_type, ref_id, created_at)` — every mutation writes a txn.
- `leftovers(id, user_id, meal_id, qty_servings, stored_at, use_by, status[available|consumed|discarded])` — conservative copy: “use within 2 days, when in doubt throw out”.
- `shopping_lists(id, user_id, title, status[open|purchased|archived])` + `shopping_list_items(id, list_id, ingredient_id, qty, unit, status[needed|in_cart|purchased], provider_hint)` — one open list auto-reused per user.

## Commerce/orders/payments/ledger (idempotent)
- `provider_configs(name PK, capabilities text[], handoff_url_template, enabled, updated_at)` — `swiggy, ondc` default `enabled=false`.
- `price_quotes(id, normalized_item, provider, price, currency[INR], unit, delivery_fee, valid_until, fetched_at, source_type[live|affiliate|user|indicative|cached], confidence)` — UI must show `source_type+fetched_at`.
- `carts(id, user_id, provider, currency, status[open|submitted|abandoned], idempotency_key UNIQUE, version)`
- `orders(id, user_id, provider, provider_order_ref, state, state_history jsonb, idempotency_key UNIQUE, total, currency, handoff_url, version)` — state machine enforced in `OrderService.transition()`.
- `payments(id, order_id/subscription_id, provider[razorpay|play], provider_payment_id UNIQUE, amount, currency, status[created|pending|paid|failed|refunded], webhook_verified BOOL, raw jsonb)` — never trust client callback.
- `ledger_entries(id, ref_type, ref_id, kind[gross|provider_fee|payment_fee|commission|refund|tax|net], amount, currency, created_at)` — accounting, not display prices.
- `subscriptions(id, user_id, plan[free|pro_monthly|pro_yearly], provider[play], purchase_token UNIQUE, status[active|grace|cancelled|expired], current_period_end, verified_at)` + `entitlements(user_id, capability, granted, source, expires_at)` materialized by `SubscriptionEntitlementService`.

## AI/notifications/admin
- `ai_conversations(id, user_id, title)` + `ai_messages(id, convo_id, role[user|assistant|tool], content, tool_name, tool_args jsonb, tokens, cost_micros, latency_ms)` + `ai_tool_calls(id, message_id, tool, args, result_ref, status)` — full audit.
- `ai_quotas(user_id, period_month, used, limit)` (Redis front, PG reconcile).
- `notifications(id, user_id, category, title, body, data jsonb, status, sent_at)` + `device_tokens(user_id, fcm_token_hash, platform)`.
- `audit_events(id, actor, action, entity, entity_id, meta jsonb, ip, created_at)` append-only; `feature_flags(key PK, enabled, payload)`; `admin_roles(user_id, role[admin|curator|support])`.

## Indexes (selection)
`meals(search_tsv GIN, name trigram GIN, diet, categories)`, `meal_history(user_id, eaten_at DESC)`, `inventory_items(user_id, status, expires_on)`, `orders(idempotency_key UNIQUE, user_id, state)`, `payments(provider_payment_id UNIQUE)`, `price_quotes(normalized_item, fetched_at DESC)`.
