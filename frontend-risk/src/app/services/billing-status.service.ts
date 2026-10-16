import { Injectable } from '@angular/core';
import { AiReadinessService } from './ai-readiness.service';

/**
 * The server's billing flag for pages that do not call /assess (/terms, /refund).
 *
 * INTERIM: the backend has no public billing-status endpoint (GET /api/ai-readiness/features only returns
 * {aiComment}), so this uses the billingEnabled of the last /assess result saved in this browser. Without a saved
 * result (or with an unmarked one) it fails closed: billing counts as off and the sale content stays hidden.
 * Proposed backend endpoint: GET /api/billing/status (public) -> { "billingEnabled": boolean }.
 */
@Injectable({ providedIn: 'root' })
export class BillingStatusService {
  constructor(private api: AiReadinessService) {}

  serverBillingEnabled(): boolean {
    return this.api.loadLastResult()?.billingEnabled === true;
  }
}
