import { environment } from '../../../environments/environment';

/** Seller and VAT values for the legal pages and the paywall card. Only these come from the environment config. */
export interface SaleConfig {
  sellerName: string;
  sellerAddress: string;
  sellerEmail: string;
  sellerRegCode: string;
  sellerVatNumber: string;
  vatLabel: string;
  lastUpdated: string;
}

const clean = (v: unknown): string => (typeof v === 'string' ? v.trim() : '');
let reportedMissing = false;

/**
 * The sale config, or null when the sale content (card, checkout, /terms sale section, /refund sale content) must
 * stay hidden. serverBillingEnabled is the SERVER's billingEnabled, the single source of truth; the environment
 * only supplies the seller name, address, email, VAT wording and date. Billing on with those missing logs
 * console.error once and hides the sale content.
 */
export function saleConfig(serverBillingEnabled: boolean,
                           env: Record<string, unknown> = environment as unknown as Record<string, unknown>): SaleConfig | null {
  if (!serverBillingEnabled) return null;
  const c: SaleConfig = {
    sellerName: clean(env['sellerName']),
    sellerAddress: clean(env['sellerAddress']),
    sellerEmail: clean(env['sellerEmail']),
    sellerRegCode: clean(env['sellerRegCode']),
    sellerVatNumber: clean(env['sellerVatNumber']),
    vatLabel: clean(env['vatLabel']),
    lastUpdated: clean(env['legalLastUpdated'])
  };
  const ready = !!c.sellerName && !!c.sellerAddress && !!c.sellerEmail && !!c.vatLabel && !!c.lastUpdated;
  if (!ready && !reportedMissing) {
    reportedMissing = true;
    console.error('FP-63: billing is on but the seller/VAT config (sellerName, sellerAddress, sellerEmail, vatLabel, '
      + 'legalLastUpdated) is incomplete; the paywall and the sale terms are hidden.');
  }
  return ready ? c : null;
}

/** Specs only: allow the one-time console.error again. */
export function resetSaleConfigReportForTests(): void {
  reportedMissing = false;
}
