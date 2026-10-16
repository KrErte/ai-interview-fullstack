import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TermsPageComponent } from './terms.page';
import { WAIVER_LABEL, WAIVER_TEXT, WAIVER_TEXT_VERSION } from '../../models/billing.model';
import { environment } from '../../../environments/environment';
import { FEATURES_URL, FILLED_SALE_CONFIG, withEnv } from './legal-test-config';

function setup() {
  TestBed.configureTestingModule({
    imports: [TermsPageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
  });
  const f = TestBed.createComponent(TermsPageComponent);
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, http: TestBed.inject(HttpTestingController) };
}
/** Renders /terms and answers /features with body (or an error when body is 'error'). */
function render(body: unknown): HTMLElement {
  const { f, el, http } = setup();
  const req = http.expectOne(FEATURES_URL);
  expect(req.request.headers.has('Authorization')).toBeFalse();
  if (body === 'error') req.flush(null, { status: 500, statusText: 'Server Error' }); else req.flush(body as object);
  f.detectChanges();
  return el;
}
const sale = (el: HTMLElement) => el.querySelector('[data-test="terms-of-sale"]');

describe('TermsPageComponent', () => {
  it('loading: general terms render at once, the sale section waits for /features', withEnv(FILLED_SALE_CONFIG, () => {
    const { el } = setup();
    expect(el.textContent).toContain('What the test is, and what it is not');
    expect(sale(el)).toBeNull();
  }));

  it('billingEnabled true + config filled: sale section with config values and the shared waiver', withEnv(FILLED_SALE_CONFIG, () => {
    const el = render({ aiComment: false, billingEnabled: true });
    expect(sale(el)!.textContent).toContain('Terms of sale – Personal AI roadmap');
    expect(sale(el)!.textContent).toContain(environment.sellerName);
    expect(sale(el)!.textContent).toContain('EE123456789');
    expect(el.querySelector('[data-test="sale-price"]')!.textContent).toContain('EUR 19 incl. VAT');
    const w = el.querySelector('[data-test="terms-waiver"]')!.textContent!;
    expect(w).toContain(WAIVER_TEXT);
    expect(w).toContain(WAIVER_LABEL);
    expect(el.textContent).not.toContain(WAIVER_TEXT_VERSION);
    expect(el.textContent).toContain('What the test is, and what it is not');
    expect(el.querySelector('[data-test="withdrawal-kept"]')!.textContent!.trim())
      .toBe('You keep your right of withdrawal until we have sent you the confirmation of your consent.');
  }));

  for (const [name, body] of [
    ['false', { aiComment: false, billingEnabled: false }],
    ['missing', { aiComment: false }],
    ['not a boolean', { aiComment: false, billingEnabled: 'true' }],
    ['an error', 'error']
  ] as const) {
    it(`billingEnabled ${name}: sale section hidden (fail closed), terms of use kept`, withEnv(FILLED_SALE_CONFIG, () => {
      const el = render(body);
      expect(sale(el)).toBeNull();
      expect(el.textContent).toContain('What the test is, and what it is not');
    }));
  }

  it('billingEnabled true but seller config missing: sale section stays hidden', () => {
    spyOn(console, 'error');
    expect(sale(render({ aiComment: false, billingEnabled: true }))).toBeNull();
  });
});
