#!/usr/bin/env bash
set -euo pipefail
echo "== secrets guard =="
if git grep -nE "OPENROUTER_API_KEY|RAZORPAY_KEY_SECRET|BEGIN PRIVATE|AIza" -- . ':!*.md' | grep -v example; then
  echo "FAIL: possible secret committed"; exit 1; fi
python3 scripts/seed_meals.py
echo "OK: static checks passed (full build runs in CI with JDK/Android SDK)."
