/** GET /api/billing/entitlement */
export interface Entitlement {
  roadmap: boolean;
}

export type BillingProduct = 'roadmap';

/** POST /api/billing/checkout body. The waiver must be true; the backend answers 400 waiver_required otherwise. */
export interface CheckoutRequest {
  product: BillingProduct;
  withdrawalWaiverAccepted: true;
}

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
