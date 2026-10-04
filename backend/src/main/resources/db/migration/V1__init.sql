-- MealWise V1 — authoritative schema (PostgreSQL 15). See docs/02-database-erd.md.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email CITEXT UNIQUE, phone_e164 TEXT UNIQUE,
  password_hash TEXT, status TEXT NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT users_identity_chk CHECK (email IS NOT NULL OR phone_e164 IS NOT NULL)
);
CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  diet TEXT NOT NULL DEFAULT 'vegetarian' CHECK (diet IN ('vegetarian','non_veg','eggetarian')),
  disliked_foods TEXT NOT NULL DEFAULT '', allergies TEXT NOT NULL DEFAULT '',
  goals_json TEXT NOT NULL DEFAULT '{}', budget_tier TEXT NOT NULL DEFAULT 'value',
  max_cook_minutes INT NOT NULL DEFAULT 30, household_size INT NOT NULL DEFAULT 2,
  cooking_skill TEXT NOT NULL DEFAULT 'beginner', updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE ingredients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name CITEXT UNIQUE NOT NULL,
  category TEXT, base_unit TEXT NOT NULL DEFAULT 'g',
  kcal_per_100g DOUBLE PRECISION NOT NULL DEFAULT 0, protein_per_100g DOUBLE PRECISION NOT NULL DEFAULT 0,
  carbs_per_100g DOUBLE PRECISION NOT NULL DEFAULT 0, fat_per_100g DOUBLE PRECISION NOT NULL DEFAULT 0,
  fibre_per_100g DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE TABLE ingredient_aliases (alias CITEXT PRIMARY KEY, ingredient_id UUID NOT NULL REFERENCES ingredients(id));
CREATE TABLE meals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), slug TEXT UNIQUE NOT NULL, name TEXT NOT NULL,
  description TEXT, diet TEXT NOT NULL CHECK (diet IN ('vegetarian','non_veg','eggetarian')),
  categories TEXT, difficulty TEXT, prep_minutes INT NOT NULL DEFAULT 10, cook_minutes INT NOT NULL DEFAULT 15,
  servings_default INT NOT NULL DEFAULT 2, image_url TEXT, tags TEXT, is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX meals_name_trgm ON meals USING gin (name gin_trgm_ops);
CREATE TABLE meal_ingredients (
  meal_id UUID NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
  ingredient_id UUID NOT NULL REFERENCES ingredients(id),
  qty_per_serving NUMERIC(12,3) NOT NULL, unit TEXT NOT NULL, preparation_note TEXT,
  is_optional BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (meal_id, ingredient_id)
);
CREATE TABLE recipe_steps (
  meal_id UUID NOT NULL REFERENCES meals(id) ON DELETE CASCADE, step_no INT NOT NULL,
  title TEXT NOT NULL, instruction TEXT NOT NULL, requires_seconds INT, temperature_c INT, tips TEXT,
  PRIMARY KEY (meal_id, step_no)
);
CREATE TABLE favourites (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  meal_id UUID NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY (user_id, meal_id)
);
CREATE TABLE meal_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  planned_for DATE NOT NULL, slot TEXT NOT NULL CHECK (slot IN ('breakfast','lunch','snack','dinner')),
  meal_id UUID REFERENCES meals(id), servings INT NOT NULL DEFAULT 2,
  status TEXT NOT NULL DEFAULT 'planned', rec_explanation TEXT, override_reason TEXT, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX meal_plans_user_date ON meal_plans (user_id, planned_for);
CREATE TABLE meal_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  meal_id UUID REFERENCES meals(id), eaten_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  servings INT NOT NULL DEFAULT 2, source TEXT NOT NULL DEFAULT 'ad_hoc'
);
CREATE INDEX meal_history_user_eaten ON meal_history (user_id, eaten_at DESC);
CREATE TABLE inventory_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  ingredient_id UUID NOT NULL REFERENCES ingredients(id),
  qty NUMERIC(12,3) NOT NULL, unit TEXT NOT NULL, qty_base DOUBLE PRECISION NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'available', expires_on DATE, source TEXT NOT NULL DEFAULT 'manual', version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX inventory_user_status ON inventory_items (user_id, status, expires_on);
CREATE TABLE shopping_lists (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'open'
);
CREATE TABLE shopping_list_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
  ingredient_id UUID REFERENCES ingredients(id), qty NUMERIC(12,3), unit TEXT, status TEXT NOT NULL DEFAULT 'needed'
);
CREATE TABLE provider_configs (name TEXT PRIMARY KEY, capabilities TEXT, enabled BOOLEAN NOT NULL DEFAULT FALSE);
INSERT INTO provider_configs VALUES ('handoff','HANDOFF',TRUE),('swiggy','HANDOFF,QUOTE,STATUS',FALSE),('ondc','SEARCH,QUOTE,STATUS,CANCEL',FALSE);
CREATE TABLE price_quotes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), normalized_item TEXT NOT NULL, provider TEXT NOT NULL,
  price_paise INT NOT NULL, delivery_fee_paise INT, currency TEXT NOT NULL DEFAULT 'INR', unit TEXT,
  source_type TEXT NOT NULL, fetched_at TIMESTAMPTZ NOT NULL DEFAULT now(), confidence DOUBLE PRECISION
);
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id),
  provider TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'CREATED',
  idempotency_key TEXT UNIQUE NOT NULL, handoff_url TEXT, state_history TEXT NOT NULL DEFAULT '[]', version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), ref_type TEXT, ref_id UUID,
  provider TEXT NOT NULL, provider_payment_id TEXT UNIQUE, amount_paise INT NOT NULL,
  currency TEXT NOT NULL DEFAULT 'INR', status TEXT NOT NULL, webhook_verified BOOLEAN NOT NULL DEFAULT FALSE, raw TEXT
);
CREATE TABLE ledger_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), ref_type TEXT NOT NULL, ref_id UUID,
  kind TEXT NOT NULL, amount_paise INT NOT NULL, currency TEXT NOT NULL DEFAULT 'INR', created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id),
  plan TEXT NOT NULL, provider TEXT NOT NULL DEFAULT 'play', purchase_token TEXT UNIQUE,
  status TEXT NOT NULL, current_period_end TIMESTAMPTZ, verified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE ai_conversations (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id), title TEXT);
CREATE TABLE ai_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), convo_id UUID REFERENCES ai_conversations(id) ON DELETE CASCADE,
  role TEXT NOT NULL, content TEXT, tool_name TEXT, tokens INT, cost_micros BIGINT, latency_ms INT
);
CREATE TABLE notifications (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id), category TEXT, title TEXT, body TEXT, status TEXT NOT NULL DEFAULT 'queued', sent_at TIMESTAMPTZ);
CREATE TABLE audit_events (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor TEXT, action TEXT NOT NULL, entity TEXT, entity_id TEXT, meta TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE feature_flags (key TEXT PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT FALSE, payload TEXT);
