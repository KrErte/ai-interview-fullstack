import { environment } from '../../../environments/environment';

/** Seller and VAT values for the legal pages, all from the environment config. */
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

/**
 * The sale config, or null when the sale content must stay hidden: billing is off, or the seller name,
 * address, email, VAT wording (vatLabel, e.g. 'incl. VAT' or 'VAT not applicable') or date are not set.
 * Read on each call, so specs can change the environment.
 */
export function saleConfig(env: Record<string, unknown> = environment as unknown as Record<string, unknown>): SaleConfig | null {
  const c: SaleConfig = {
    sellerName: clean(env['sellerName']),
    sellerAddress: clean(env['sellerAddress']),
    sellerEmail: clean(env['sellerEmail']),
    sellerRegCode: clean(env['sellerRegCode']),
    sellerVatNumber: clean(env['sellerVatNumber']),
    vatLabel: clean(env['vatLabel']),
    lastUpdated: clean(env['legalLastUpdated'])
  };
  const ready = env['billingEnabled'] === true && !!c.sellerName && !!c.sellerAddress && !!c.sellerEmail
    && !!c.vatLabel && !!c.lastUpdated;
  return ready ? c : null;
}
