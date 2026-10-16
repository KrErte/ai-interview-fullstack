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
| `APP_PUBLIC_BASE_URL` (`app.public-base-url`) | `http://localhost:4200` (falls back to `FRONTEND_BASE_URL`) | The only base of the checkout success/cancel urls (trailing slash stripped); never taken from the request `Origin`/`Host`. |
| `STRIPE_WEBHOOK_SECRET` | empty | Signing secret (`whsec_…`) of the webhook endpoint (FP-62). Empty: every webhook is rejected with 400. Never logged. |
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

Production fail-fast: with the `prod` profile and `BILLING_ENABLED=true` the app refuses to start unless
`STRIPE_SECRET_KEY`, `STRIPE_PRICE_ID` and `STRIPE_WEBHOOK_SECRET` are set (production always charges the Stripe Price,
never `price_data`) and `APP_PUBLIC_BASE_URL` (or `FRONTEND_BASE_URL`) is an explicit `https` url whose host is not
localhost/127.0.0.1. The prod profile has no localhost default for it, so an unset url fails startup instead of
sending buyers to localhost.

### `POST /api/billing/checkout` (login required, FP-61)

Body `{"product": "roadmap", "withdrawalWaiverAccepted": true, "withdrawalWaiverVersion": "waiver-v1-2026-10-16"}`.
Response `{"url": "https://checkout.stripe.com/…"}`; the frontend redirects the browser there.

| Status | When |
|---|---|
| 200 `{"url": …}` | Stripe Checkout session created |
| 400 `{"error": "unknown_product"}` | `product` missing or not `roadmap` |
| 400 `{"error": "waiver_required"}` | `withdrawalWaiverAccepted` is not the JSON boolean `true` (the string `"true"` or `1` do not count) |
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
   `metadata` `{purchase_id, product}` (also on the PaymentIntent), `payment_intent_data.receipt_email` = the
   user's email (Stripe emails the receipt),
   `success_url` `{APP_PUBLIC_BASE_URL}/roadmap?paid=1&session_id={CHECKOUT_SESSION_ID}` (Stripe's literal
   placeholder, filled in by Stripe), `cancel_url` `{APP_PUBLIC_BASE_URL}/ai-test?resume=paywall&canceled=1`.
3. The session id is stored in `purchase.stripe_session_id`. A reused row gets the new id; a payment of its older
   session is still matched by `metadata.purchase_id`.

The waiver confirmation (`WithdrawalWaiver`, one fixed text per version, e.g. "Roadmap. Customer agreed to immediate
delivery and waived the 14-day right of withdrawal (waiver-v1-2026-10-16).") is sent as
`payment_intent_data.description`, which Stripe prints on the emailed receipt. No `invoice_creation` (no invoice, no
invoice fee). **The waiver wording is still pending legal review**; a changed wording needs a new version.

Stripe API version: pinned explicitly to the version bundled with stripe-java (`Stripe.API_VERSION`, currently
`2026-09-30.endive` for 34.0.0) on every checkout request (`StripeCheckoutGateway.API_VERSION`). Upgrading the SDK
moves it, so read the Stripe changelog then. The webhook reads raw JSON and does not depend on it; set the webhook
endpoint's API version in the Stripe dashboard to the same value.

The success redirect never unlocks anything and the backend never reads `session_id`; `/roadmap?paid=1` should
poll `GET /api/billing/entitlement`. Only the
webhook (FP-62) marks a purchase `paid`.

### `POST /api/billing/webhook` (public, Stripe only, FP-62)

Listed as `permitAll` in `SecurityConfig` (POST only); CSRF is disabled app-wide (stateless JWT). The raw body is
verified against the `Stripe-Signature` header with `STRIPE_WEBHOOK_SECRET` (`Webhook.constructEvent`, default
300 s tolerance).

| Status | When |
|---|---|
| 400 `{"error": "invalid_signature"}` | missing/invalid/expired signature, or no secret configured. No details. |
| 200 `{"received": true}` | every verified event, also unknown sessions, duplicates and ignored types |

Handled: `checkout.session.completed` and `checkout.session.async_payment_succeeded` with `payment_status = "paid"`.
The purchase is found by `stripe_session_id` (fallback: `metadata.purchase_id`) and, if `pending`, set to `paid` with
`paid_at`, `buyer_country` (`customer_details.address.country`), and `amount_cents`/`currency` from the session's
`amount_total`/`currency`. Already `paid` → no change. `refunded` → no change. Unknown session → 200, logged by
session id only. On PostgreSQL, a clash with the one-paid-row-per-user+product index (a second payment) is treated
as already paid and logged for a manual refund. Other events and `payment_status` `unpaid` → 200, ignored.
Logs carry event/session/purchase ids only, never email, name or keys.

Configure the endpoint in Stripe (test mode) for `https://<backend>/api/billing/webhook` with at least the two
event types above.

## Local testing (Stripe test mode)

```bash
# .env: BILLING_ENABLED=true, STRIPE_SECRET_KEY=sk_test_..., STRIPE_PRICE_ID=price_... (test mode)
stripe login
stripe listen --forward-to localhost:8080/api/billing/webhook   # prints whsec_... -> STRIPE_WEBHOOK_SECRET, restart
# log in, then
curl -s -X POST localhost:8080/api/billing/checkout -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"product":"roadmap","withdrawalWaiverAccepted":true,"withdrawalWaiverVersion":"waiver-v1-2026-10-16"}'
# open the url, pay with 4242 4242 4242 4242; then
curl -s localhost:8080/api/billing/entitlement -H "Authorization: Bearer $TOKEN"   # {"roadmap":true}
```

Without the Stripe CLI, a signed event can be faked with the same secret the backend uses:

```bash
SECRET=whsec_local_test   # = STRIPE_WEBHOOK_SECRET of the running backend
BODY='{"id":"evt_local","object":"event","type":"checkout.session.completed","data":{"object":{"id":"cs_test_...","object":"checkout.session","payment_status":"paid","amount_total":1900,"currency":"eur","metadata":{"purchase_id":"1"},"customer_details":{"address":{"country":"EE"}}}}}'
T=$(date +%s)
SIG=$(printf '%s' "$T.$BODY" | openssl dgst -sha256 -hmac "$SECRET" -hex | sed 's/^.* //')
curl -s -X POST localhost:8080/api/billing/webhook -H "Stripe-Signature: t=$T,v1=$SIG" \
  -H 'Content-Type: application/json' --data-raw "$BODY"
```

Tests do the same in `StripeSignatureFixture` (no network).
