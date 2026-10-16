# Billing and the roadmap paywall (FP-60)

Everything is controlled by `BILLING_ENABLED` (property `billing.enabled`, default `false`).
While it is `false`, the API behaves as before billing existed.

## Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `BILLING_ENABLED` | `false` | Turns the paywall on. Off: nothing is gated or stripped. |
| `STRIPE_PRICE_ID` | empty | Stripe Price charged by checkout (FP-61). Required in production (see below). |
| `STRIPE_SECRET_KEY` | empty | Stripe secret key. TEST mode (`sk_test_…`) everywhere but production. Empty: checkout answers 503. Never logged. |
| `BILLING_FALLBACK_AMOUNT_CENTS` / `BILLING_CURRENCY` | `1900` / `eur` | Only while `STRIPE_PRICE_ID` is empty (local/dev): sent as Stripe `price_data`. Also the expected amount stored on the pending purchase until the webhook (FP-62) records what Stripe charged. |
| `FRONTEND_BASE_URL` | `http://localhost:4200` | Base of the checkout success/cancel urls. |
| `STRIPE_CONNECT_TIMEOUT_MS` / `STRIPE_READ_TIMEOUT_MS` | `5000` / `20000` | Stripe HTTP timeouts; no automatic retries. |
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

Production fail-fast: with the `prod` profile and `BILLING_ENABLED=true` the app refuses to start unless both
`STRIPE_SECRET_KEY` and `STRIPE_PRICE_ID` are set (production always charges the Stripe Price, never `price_data`).

### `POST /api/billing/checkout` (login required, FP-61)

Body `{"product": "roadmap", "withdrawalWaiverAccepted": true, "withdrawalWaiverVersion": "waiver-v1-2026-10-16"}`.
Response `{"url": "https://checkout.stripe.com/…"}`; the frontend redirects the browser there.

| Status | When |
|---|---|
| 200 `{"url": …}` | Stripe Checkout session created |
| 400 `{"error": "unknown_product"}` | `product` missing or not `roadmap` |
| 400 `{"error": "waiver_required"}` | `withdrawalWaiverAccepted` is not `true` |
| 400 `{"error": "invalid_waiver_version"}` | version is not exactly `BillingSettings.CURRENT_WAIVER_VERSION` |
| 400 `{"error": "invalid_request"}` | unreadable JSON |
| 401 | no valid login |
| 404 `{"error": "billing_disabled"}` | `BILLING_ENABLED=false` |
| 409 `{"error": "already_paid"}` | the user already has the roadmap (paid, or dev-entitled) |
| 503 `{"error": "payment_provider_unavailable"}` | Stripe unreachable, timed out, refused, or no secret key |

Checks run in that order (billing, product, waiver, version, entitlement). Then:

1. A `pending` purchase row is stored (or the user's latest pending row for the product is reused) with
   `withdrawal_waiver_accepted = true`, `withdrawal_waiver_accepted_at`, `waiver_text_version`, `price_id` and the
   expected `amount_cents`/`currency`.
2. A Stripe Checkout session is created: `mode=payment`, one line item (`price` = `STRIPE_PRICE_ID`, or `price_data`
   from the fallback amount/currency), `customer_email` = the user's email, `client_reference_id` = the user id,
   `metadata` `{purchase_id, product}` (also on the PaymentIntent and invoice),
   `success_url` `{FRONTEND_BASE_URL}/roadmap?paid=1`, `cancel_url` `{FRONTEND_BASE_URL}/risk`.
3. The session id is stored in `purchase.stripe_session_id`. A reused row gets the new id; a payment of its older
   session is still matched by `metadata.purchase_id`.

The waiver confirmation (`WithdrawalWaiver`, one fixed text per version, e.g. "Roadmap. Customer agreed to immediate
delivery and waived the 14-day right of withdrawal (waiver-v1-2026-10-16).") is sent as
`payment_intent_data.description` and, with `invoice_creation.enabled=true` (supported only for `mode=payment`), as the
invoice footer, so Stripe's receipt/invoice records it. Note: Stripe charges a fee for post-payment invoices on
Checkout; drop `invoice_creation` if that is unwanted (the description alone shows on the receipt).

The success redirect never unlocks anything; `/roadmap?paid=1` should poll `GET /api/billing/entitlement`. Only the
webhook (FP-62) marks a purchase `paid`.

## Not built yet

- FP-62 webhook: marks the purchase paid, idempotent on `stripe_session_id`.
