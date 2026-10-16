import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CheckoutFailure, CheckoutResponse, Entitlement } from '../models/billing.model';

/** Billing API (auth only). Contract: FP-63 brief; the backend side is not built yet. */
@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/billing`;

  constructor(private http: HttpClient) {}

  getEntitlement(): Observable<Entitlement> {
    return this.http.get<Entitlement>(`${this.baseUrl}/entitlement`);
  }

  startCheckout(): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.baseUrl}/checkout`, {
      product: 'roadmap',
      withdrawalWaiverAccepted: true
    });
  }

  /** 409 = already paid, 400 waiver_required = checkbox missing, anything else (incl. network) = other. */
  static classifyCheckoutError(err: unknown): CheckoutFailure {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 409) return 'already_paid';
      if (err.status === 400 && err.error?.error === 'waiver_required') return 'waiver_required';
    }
    return 'other';
  }
}

/** True for the contract's 402 {"error":"payment_required"} (any 402 counts, the body may be missing behind a proxy). */
export function isPaymentRequired(err: unknown): boolean {
  return err instanceof HttpErrorResponse && err.status === 402;
}
