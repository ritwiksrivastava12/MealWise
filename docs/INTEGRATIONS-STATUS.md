# INTEGRATIONS STATUS — honest availability (no fabricated capability)

| Integration | Status | What works now | Blocker / next step |
|---|---|---|---|
| Email+password auth | ✅ Live (backend) | Register/login/refresh/logout, Argon2id, rate-limit | Needs SMTP (SendGrid/SES) for reset emails in staging |
| Phone OTP | 🟡 Wired, needs Firebase project | Android Firebase Auth + backend token verify code present | Provide `google-services.json` + enable Phone provider |
| Meal catalog | ✅ Live | 100 seeded Indian meals, search/filters/favourites | Admin curation continues |
| OpenRouter AI | 🟡 Gateway live, needs key | Tools, quotas, fallback compile; WireMock tests pass | Set `OPENROUTER_API_KEY` + `AI_MODEL_*`; no chat without it — UI shows “AI unavailable (key missing)” not fake replies |
| Vision | 🟡 Code present, model-gated | Upload + suggest + confirm flow | Needs vision model ID + storage bucket policy |
| Swiggy | 🔴 Handoff only | Deep-link search URLs with attribution + disclaimer | Needs Swiggy Builders/MCP prod approval — do not use staging keys in prod |
| ONDC | 🔴 Handoff only | Adapter interface + protocol TODOs mapped to spec | Needs buyer registration + current spec + signing keys |
| Razorpay | 🟡 Sandbox-ready | Order create + HMAC webhook verify + idempotency (test keys) | Needs live `RAZORPAY_KEY_ID/SECRET` + webhook URL registered |
| Play Billing Pro | 🟡 Wired, needs Console | BillingClient flow + server verify + RTDN handler code | Needs service-account JSON + RTDN topic + test-track purchase |
| FCM push | 🟡 Wired, needs sender | Token register + preference-gated send service | Provide `FCM_PROJECT_ID` + server key (Admin SDK) |
| Live grocery prices | 🔴 Unavailable by design | PriceIntel stores only `test/user/indicative` until authorized | UI never shows live prices; shows “No live price — check provider” |

Legend: ✅ live · 🟡 works in sandbox/needs credential · 🔴 intentionally unavailable (lawful fallback shown).
