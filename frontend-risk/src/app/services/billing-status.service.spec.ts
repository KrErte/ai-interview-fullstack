import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BillingStatusService } from './billing-status.service';
import { environment } from '../../environments/environment';

const URL = `${environment.apiBaseUrl}/api/ai-readiness/features`;

describe('BillingStatusService', () => {
  let svc: BillingStatusService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    svc = TestBed.inject(BillingStatusService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  function value(body: unknown): boolean | undefined {
    let v: boolean | undefined;
    svc.billingEnabled().subscribe(x => (v = x));
    const req = http.expectOne(URL);
    if (body === 'error') req.flush(null, { status: 500, statusText: 'Server Error' }); else req.flush(body as object);
    return v;
  }

  it('true', () => expect(value({ aiComment: false, billingEnabled: true })).toBeTrue());
  it('false', () => expect(value({ aiComment: false, billingEnabled: false })).toBeFalse());
  it('missing', () => expect(value({ aiComment: false })).toBeFalse());
  it('not a boolean', () => expect(value({ aiComment: false, billingEnabled: 'yes' })).toBeFalse());
  it('error', () => expect(value('error')).toBeFalse());

  it('one request per app session (cached)', () => {
    expect(value({ aiComment: true, billingEnabled: true })).toBeTrue();
    let again: boolean | undefined;
    svc.billingEnabled().subscribe(x => (again = x));
    http.expectNone(URL);
    expect(again).toBeTrue();
  });
});
