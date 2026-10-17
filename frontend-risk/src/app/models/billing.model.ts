/** GET /api/billing/entitlement */
export interface Entitlement {
  roadmap: boolean;
}

export type BillingProduct = 'roadmap';

/** POST /api/billing/checkout body. The waiver must be true; the backend answers 400 waiver_required otherwise. */
export interface CheckoutRequest {
  product: BillingProduct;
  withdrawalWaiverAccepted: true;
  /** Which wording the user ticked (WAIVER_TEXT_VERSION); to be stored with the order (FP-65). */
  withdrawalWaiverVersion: string;
}

/**
 * The withdrawal waiver (FP-65, drafts/paywall-copy.md). ONE versioned text: the paywall checkbox shows
 * exactly WAIVER_TEXT, /terms section 6 quotes the same constant, and the checkout sends WAIVER_TEXT_VERSION.
 * Change the version id whenever the wording changes.
 */
export const WAIVER_TEXT_VERSION = 'waiver-v1-2026-10-16';
/** Human-readable version shown with the waiver; the ID (WAIVER_TEXT_VERSION) stays internal and is only sent. */
export const WAIVER_LABEL = 'Version 1, 16 October 2026';
export const WAIVER_TEXT =
  'I want my personal AI roadmap delivered immediately, and I agree that delivery starts before the 14-day withdrawal ' +
  'period ends. I understand that I lose my right of withdrawal once delivery has started.';

/** POST /api/billing/checkout response: the hosted checkout page to send the browser to. */
export interface CheckoutResponse {
  url: string;
}

/** Error bodies in the billing contract: 400 waiver_required, 402 payment_required (roadmap endpoint). */
export interface BillingErrorBody {
  error: 'waiver_required' | 'payment_required' | string;
}

/** What a failed checkout means for the UI. */
export type CheckoutFailure = 'already_paid' | 'waiver_required' | 'unavailable' | 'other';

/**
 * Where checkout sends the browser back (FP-64). ASSUMED: the backend builds the Stripe success/cancel URLs
 * itself; the checkout request has no URL fields. Proposed: success_url = <origin>/roadmap?paid=1&session_id=
 * {CHECKOUT_SESSION_ID}, cancel_url = <origin>/ai-test?resume=paywall&canceled=1.
 */
export const CHECKOUT_SUCCESS_PATH = '/roadmap';
export const CHECKOUT_CANCEL_PATH = '/ai-test';
