import { environment } from '../../../environments/environment';
import { resetSaleConfigReportForTests } from './legal-config';

/** Test helpers: a filled seller/VAT config, and wrappers that set environment keys and the server flag for one spec. */
export const FILLED_SALE_CONFIG = {
  sellerName: 'Test Seller OÜ',
  sellerAddress: 'Test street 1, Tallinn',
  sellerEmail: 'sales@example.test',
  sellerRegCode: '12345678',
  sellerVatNumber: 'EE123456789',
  vatLabel: 'incl. VAT',
  legalLastUpdated: '16 October 2026'
};

export function withEnv(values: Record<string, unknown>, fn: () => void): () => void {
  return () => {
    const env = environment as unknown as Record<string, unknown>;
    const saved: Record<string, unknown> = {};
    for (const k of Object.keys(values)) saved[k] = env[k];
    Object.assign(env, values);
    resetSaleConfigReportForTests();
    try { fn(); } finally { Object.assign(env, saved); }
  };
}

/**
 * Env values plus the server's billingEnabled as /terms and /refund see it today (interim: the last saved /assess
 * result, see BillingStatusService).
 */
export function withSale(values: Record<string, unknown>, serverBilling: boolean, fn: () => void): () => void {
  return withEnv(values, () => {
    localStorage.setItem('ai_readiness_last_result', JSON.stringify({
      professionId: 'onet-1', professionName: 'Accountant', billingMarker: 'v1', billingEnabled: serverBilling,
      roadmapLocked: serverBilling, generalRoadmap: [], professionRoadmap: [], tasks: []
    }));
    try { fn(); } finally { localStorage.removeItem('ai_readiness_last_result'); }
  });
}
