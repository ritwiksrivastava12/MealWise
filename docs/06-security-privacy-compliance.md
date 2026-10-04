# Security, privacy & India compliance (implement now, counsel must review before launch)

## AuthN/Z
- Argon2id (`Argon2PasswordEncoder` defaults 16/32/1/1) for passwords; Firebase Admin SDK verifies phone ID tokens; JWT access (EdDSA/HS256 via KMS-managed secret rotation) 15m + opaque refresh (SHA-256 stored, rotation, device binding, revocation list in Redis).
- Rate limits: login 5/min/IP+account, OTP 3/10min, AI 60/min/user, webhook 200/min/source-IP allowlist. Account lockout with exponential backoff; suspicious-login email + device review.
- RBAC: `ROLE_USER`, `ROLE_CURATOR` (meals), `ROLE_SUPPORT` (read-only PII-masked), `ROLE_ADMIN`. Admin APIs under `/admin/**`, deny-by-default, audit every call.

## Data protection
- TLS 1.2+ everywhere, HSTS, EncryptedSharedPreferences/DataStore (Keystore) on device, AES-256-GCM for PII columns where needed (`phone`, `fcm_token` hashed), PG encryption-at-rest (managed disk), backups encrypted.
- Secrets via env/secret manager (GCP SM / AWS SM); no secrets in git (CI `gitleaks`), no secrets in APK (Play Asset / server-driven config only).
- Logging: structured JSON, `user_id` hashed in AI logs, never log passwords/tokens/card data/full phone. Image uploads: 5 MB cap, MIME sniff (not extension), EXIF stripped, AV-scan hook, retention 30d or user-deleted sooner.

## DPDP Act 2023 readiness (not legal advice — get counsel sign-off)
- Consent: onboarding + settings show purpose-limited consent (personalization, notifications, AI processing incl. OpenRouter as processor, payments). Granular toggles; withdrawal = feature degrade, not lockout.
- Data Principal rights: export (`GET /users/me/export`), correction (profile/preferences PATCH), erasure (`DELETE /users/me` → soft-delete + 30d purge job + processor deletion requests), grievance redressal contact in-app + privacy policy URL (required for Play).
- Minimization/retention: history 365d (Pro)/30d aggregated after, AI messages 180d, webhook raws 2y (financial), audit 5y append-only. Children: 18+ at launch (age gate in onboarding).
- Processors disclosed: OpenRouter (AI), Firebase (auth/push), Razorpay (payments), Google Play (billing), hosting provider. DPA review per vendor.

## Threat model (top risks → control)
Prompt-injection via menu/bill images → untrusted tagging + tool-only facts. Price spoof → PriceIntel source labelling + server fetch only. Payment forgery → webhook HMAC + server fetch. Entitlement bypass → server verify + interceptor. Mass AI cost → quotas + caps + alerts. Scraping → no scraping by policy + WAF/rate-limit.
