import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { AiReadinessService } from './ai-readiness.service';
import { Features } from '../models/ai-readiness.model';

/**
 * The server's billingEnabled from the public, anonymous GET /api/ai-readiness/features (FP-60), the single
 * source of truth for the paywall card, /terms and /refund. One request per app session, shared by everyone.
 * Fails closed: an error, a missing value or a non-boolean counts as billing off.
 */
@Injectable({ providedIn: 'root' })
export class BillingStatusService {
  private features$?: Observable<Features | null>;

  constructor(private api: AiReadinessService) {}

  /** The cached /features response; null when the request failed. */
  features(): Observable<Features | null> {
    if (!this.features$) {
      this.features$ = this.api.features().pipe(
        catchError(() => of(null)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.features$;
  }

  billingEnabled(): Observable<boolean> {
    return this.features().pipe(map(f => f !== null && typeof f === 'object' && f.billingEnabled === true));
  }
}
