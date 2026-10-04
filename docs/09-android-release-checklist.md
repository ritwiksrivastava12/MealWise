# Android release checklist (verify against current Play policy before submit)

- [ ] App ID `in.mealwise.app`, versionCode/semver, `minSdk 26 / targetSdk 34`, R8 + shrink + obfuscation smoke test.
- [ ] Adaptive icon + monochrome, splash (`core-splashscreen`), feature graphics, 3D/Lottie lazy-loaded (on-demand module if >5 MB).
- [ ] Permissions minimal: INTERNET, POST_NOTIFICATIONS (runtime), CAMERA (optional vision only). No location unless ordering needs it (justify in Data Safety).
- [ ] Data Safety form: email/phone, inventory, AI messages (OpenRouter processor), payments (Razorpay/Play — no card storage), crash logs. Link privacy policy + deletion (`/users/me/export`, `/users/me` delete).
- [ ] Play Billing 7 for Pro (no Razorpay for unlock). Prices in INR, trial/intro configured in Console. RTDN endpoint live + verified.
- [ ] Deep links/app links (`mealwise://meal/{id}`, `https://mealwise.in/...`), FCM channel prefs, crash (Crashlytics) + ANR monitoring.
- [ ] Signing: Play App Signing, upload key in HSM. Release tracks: internal → closed → production (staged 20%).
- [ ] Legal: privacy policy, terms, DPDP consent + grievance officer, 18+ age gate, “estimates / general wellness” copy audit.
- [ ] Perf: baseline profiles, startup <2.5s Moto G, no heavy 3D on home background.
