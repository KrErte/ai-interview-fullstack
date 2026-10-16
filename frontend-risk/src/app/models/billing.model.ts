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
/** Human label shown with the waiver in the checkbox, Terms §6 and the receipt. */
export const WAIVER_LABEL = 'Waiver v1, 2026-10-16';
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
export type CheckoutFailure = 'already_paid' | 'waiver_required' | 'other';
