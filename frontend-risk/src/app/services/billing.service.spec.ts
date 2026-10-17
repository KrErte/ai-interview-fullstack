import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BillingService, isPaymentRequired } from './billing.service';
import { environment } from '../../environments/environment';
import { WAIVER_TEXT_VERSION } from '../models/billing.model';

describe('BillingService', () => {
  let service: BillingService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/billing`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BillingService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('GETs the entitlement', () => {
    let got: unknown;
    service.getEntitlement().subscribe(e => (got = e));
    const req = http.expectOne(`${base}/entitlement`);
    expect(req.request.method).toBe('GET');
    req.flush({ roadmap: true });
    expect(got).toEqual({ roadmap: true });
  });

  it('POSTs the checkout with the roadmap product and the waiver, returning the url', () => {
    let url = '';
    service.startCheckout().subscribe(r => (url = r.url));
    const req = http.expectOne(`${base}/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ product: 'roadmap', withdrawalWaiverAccepted: true, withdrawalWaiverVersion: WAIVER_TEXT_VERSION });
    req.flush({ url: 'https://pay.example/session' });
    expect(url).toBe('https://pay.example/session');
  });

  it('surfaces 400 / 409 errors and classifies them', () => {
    const errors: HttpErrorResponse[] = [];
    service.startCheckout().subscribe({ error: e => errors.push(e) });
    http.expectOne(`${base}/checkout`).flush({ error: 'waiver_required' }, { status: 400, statusText: 'Bad Request' });
    service.startCheckout().subscribe({ error: e => errors.push(e) });
    http.expectOne(`${base}/checkout`).flush(null, { status: 409, statusText: 'Conflict' });
    service.startCheckout().subscribe({ error: e => errors.push(e) });
    http.expectOne(`${base}/checkout`).flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });
    service.startCheckout().subscribe({ error: e => errors.push(e) });
    http.expectOne(`${base}/checkout`).error(new ProgressEvent('error'));

    expect(errors.map(e => BillingService.classifyCheckoutError(e)))
      .toEqual(['waiver_required', 'already_paid', 'other', 'other']);
  });

  it('treats a 400 with another error code as other', () => {
    const e = new HttpErrorResponse({ status: 400, error: { error: 'bad_product' } });
    expect(BillingService.classifyCheckoutError(e)).toBe('other');
  });

  it('isPaymentRequired is true only for 402', () => {
    expect(isPaymentRequired(new HttpErrorResponse({ status: 402, error: { error: 'payment_required' } }))).toBeTrue();
    expect(isPaymentRequired(new HttpErrorResponse({ status: 401 }))).toBeFalse();
    expect(isPaymentRequired(new Error('x'))).toBeFalse();
  });
});

describe('BillingService.classifyCheckoutError (FP-64)', () => {
  it('404 billing_disabled and 503 payment_provider_unavailable are "unavailable"; plain 404/503 are "other"', async () => {
    const { HttpErrorResponse: E } = await import('@angular/common/http');
    expect(BillingService.classifyCheckoutError(new E({ status: 404, error: { error: 'billing_disabled' } }))).toBe('unavailable');
    expect(BillingService.classifyCheckoutError(new E({ status: 503, error: { error: 'payment_provider_unavailable' } }))).toBe('unavailable');
    expect(BillingService.classifyCheckoutError(new E({ status: 404 }))).toBe('other');
    expect(BillingService.classifyCheckoutError(new E({ status: 503 }))).toBe('other');
  });
});
