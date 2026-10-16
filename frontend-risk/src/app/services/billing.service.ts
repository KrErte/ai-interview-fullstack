import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, exhaustMap, first, map, of, take, timer } from 'rxjs';
import { environment } from '../../environments/environment';
import { CheckoutFailure, CheckoutRequest, CheckoutResponse, Entitlement, WAIVER_TEXT_VERSION } from '../models/billing.model';

/** Billing API (auth only). Contract: FP-63 brief; the backend side is not built yet. */
@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/billing`;

  constructor(private http: HttpClient) {}

  getEntitlement(): Observable<Entitlement> {
    return this.http.get<Entitlement>(`${this.baseUrl}/entitlement`);
  }

  startCheckout(): Observable<CheckoutResponse> {
    const body: CheckoutRequest = {
      product: 'roadmap',
      withdrawalWaiverAccepted: true,
      withdrawalWaiverVersion: WAIVER_TEXT_VERSION
    };
    return this.http.post<CheckoutResponse>(`${this.baseUrl}/checkout`, body);
  }

  /**
   * After the checkout return: asks GET /entitlement every intervalMs (first at once) for up to maxMs. Emits true
   * as soon as it says roadmap:true, or false when the time is up; then completes. Failed checks count as "not
   * yet". Unsubscribing stops the polling.
   */
  pollEntitlement(intervalMs = 1000, maxMs = 15000): Observable<boolean> {
    return timer(0, intervalMs).pipe(
      take(Math.floor(maxMs / intervalMs) + 1),
      exhaustMap(() => this.getEntitlement().pipe(
        map(e => e?.roadmap === true),
        catchError(() => of(false))
      )),
      first(ok => ok, false)
    );
  }

  /**
   * 409 = already paid, 400 waiver_required = checkbox missing, 404 billing_disabled / 503
   * payment_provider_unavailable = unavailable, anything else (incl. network) = other.
   */
  static classifyCheckoutError(err: unknown): CheckoutFailure {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 409) return 'already_paid';
      if (err.status === 400 && err.error?.error === 'waiver_required') return 'waiver_required';
      if ((err.status === 404 && err.error?.error === 'billing_disabled')
        || (err.status === 503 && err.error?.error === 'payment_provider_unavailable')) return 'unavailable';
    }
    return 'other';
  }
}

/** True for the contract's 402 {"error":"payment_required"} (any 402 counts, the body may be missing behind a proxy). */
export function isPaymentRequired(err: unknown): boolean {
  return err instanceof HttpErrorResponse && err.status === 402;
}
