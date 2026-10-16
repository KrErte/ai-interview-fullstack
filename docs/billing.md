# Billing and the roadmap paywall (FP-60)

Everything is controlled by `BILLING_ENABLED` (property `billing.enabled`, default `false`).
While it is `false`, the API behaves as before billing existed.

## Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `BILLING_ENABLED` | `false` | Turns the paywall on. Off: nothing is gated or stripped. |
| `STRIPE_PRICE_ID` | empty | Stripe Price used by checkout (FP-61). No amount is configured in code; `purchase.amount_cents`/`currency` only record what Stripe charged. |
| `BILLING_DISPLAY_PRICE` | `€19` | Price text on public teasers (job pages, FP-66). Display only; keep it in sync with the Stripe Price. |
| `BILLING_DEV_ENTITLEMENT` | `false` | Test/dev switch, independent of the Spring profile. `true`: ADMIN users and `BILLING_DEV_ENTITLED_EMAILS` count as paid. `false`: nobody gets access without a paid purchase. Never set it in production. |
| `BILLING_DEV_ENTITLED_EMAILS` | empty | Comma-separated emails treated as paid, only while `BILLING_DEV_ENTITLEMENT=true`. |

Only a `purchase` row with `status = 'paid'` and `product = 'roadmap'` for the logged-in user grants access
(`pending` and `refunded` do not). On PostgreSQL a partial unique index allows at most one paid row per user and product.

## Endpoints

### `POST /api/ai-readiness/assess` (public)

Billing **off**: unchanged, plus `"roadmapLocked": false, "billingEnabled": false`.

Billing **on** (for everyone, also paying or logged-in users):

- removed: `generalRoadmap`, `professionRoadmap`, `totalWeeks`
- added: `"roadmapLocked": true`, `"billingEnabled": true` and

```json
"roadmapTeaser": [
  {"phase": 1, "title": "…", "items": ["…"], "project": "…"},
  {"phase": 2, "title": "…"},
  {"phase": 3, "title": "…"}
]
```

Phases 2+ carry only `phase` and `title` (no `items`/`project` keys). Fewer phases give a shorter array.

### `POST /api/ai-readiness/roadmap` (login required)

Body `{"professionId": "translator", "answers": {…}}` (`professionId` required, max 128 UTF-16 code units, so a character outside the BMP such as an emoji counts as 2; `answers` required).
An unknown profession is echoed in the 400 message cut to 64 code points plus "…".
Response `{"generalRoadmap": […], "professionRoadmap": […], "totalWeeks": 37}`.

| Status | When |
|---|---|
| 200 | billing off (any logged-in user), or billing on and paid / dev-entitled |
| 400 `{"error": "…"}` | bad input or unknown profession (checked before payment) |
| 401 | no valid login |
| 402 `{"error": "payment_required"}` | billing on and not entitled |

### `GET /api/ai-readiness/features` (public)

`{"aiComment": true|false, "billingEnabled": true|false}`; `billingEnabled` mirrors `BILLING_ENABLED`, so the
frontend knows whether to expect the teaser shape from `/assess`.

### `GET /api/billing/entitlement` (login required)

`{"roadmap": true|false}`; always `true` while billing is off. 401 without login.

## Not built yet

- FP-61 checkout: body includes `withdrawalWaiverVersion` (only `waiver-v1-2026-10-16`, see
  `BillingSettings.WAIVER_VERSIONS`), stored in `purchase.waiver_text_version`; the Stripe receipt must carry the
  waiver confirmation.
- FP-62 webhook: idempotent on `stripe_session_id`.
