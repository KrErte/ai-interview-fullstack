import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PrivacyPageComponent } from './privacy.page';
import { FEATURES_URL, FILLED_SALE_CONFIG, withEnv } from './legal-test-config';
import { environment } from '../../../environments/environment';

const STATUS_URL = `${environment.apiBaseUrl}/api/ai-readiness/mock-interview/status`;

/** Renders /privacy, answers /features with body (or an error when body is 'error') and the mock interview status. */
function render(body: unknown, provider: 'anthropic' | 'openai' | null = null): HTMLElement {
  TestBed.configureTestingModule({
    imports: [PrivacyPageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
  });
  const f = TestBed.createComponent(PrivacyPageComponent);
  f.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  const req = http.expectOne(FEATURES_URL);
  if (body === 'error') req.flush(null, { status: 500, statusText: 'Server Error' }); else req.flush(body as object);
  http.expectOne(STATUS_URL).flush({ provider });
  f.detectChanges();
  return f.nativeElement as HTMLElement;
}
const payments = (el: HTMLElement) => el.querySelector('[data-test="privacy-payments"]');

describe('PrivacyPageComponent', () => {
  const OFF = { aiComment: false, billingEnabled: false };

  it('mock interview with Claude: names Anthropic as the receiver and processor', () => {
    const el = render(OFF, 'anthropic');
    const section = el.querySelector('[data-test="privacy-mock"]')!.nextElementSibling!.textContent!;
    expect(section).toContain('we send Anthropic (Claude) your profession');
    expect(section).toContain('does not use data sent through its API');
    expect(el.textContent).toContain('Anthropic receives data only for the AI mock interview');
    expect(el.textContent).not.toContain('optional AI comment and the AI mock interview');
  });

  it('mock interview with OpenAI: names OpenAI for it', () => {
    const el = render(OFF, 'openai');
    expect(el.querySelector('[data-test="privacy-mock"]')!.nextElementSibling!.textContent).toContain('we send OpenAI your profession');
    expect(el.textContent).toContain('optional AI comment and the AI mock interview');
    expect(el.textContent).not.toContain('Anthropic');
  });

  it('mock interview off: the policy does not describe it', () => {
    const el = render(OFF, null);
    expect(el.querySelector('[data-test="privacy-mock"]')).toBeNull();
    expect(el.textContent).not.toContain('Anthropic');
  });

  it('billing on + seller config: describes purchases, Stripe and how long purchase records are kept', withEnv(FILLED_SALE_CONFIG, () => {
    const el = render({ aiComment: false, billingEnabled: true });
    expect(payments(el)).not.toBeNull();
    expect(el.textContent).toContain('The seller, Test Seller OÜ, is the controller for this data.');
    expect(el.textContent).toContain('we never see or store them');
    expect(el.textContent).toContain('processes payments for the full roadmap');
    expect(el.querySelector('[data-test="privacy-purchase-retention"]')).not.toBeNull();
    expect(el.textContent).toContain('Team reports');
  }));

  for (const [name, body] of [
    ['billing off', { aiComment: false, billingEnabled: false }],
    ['/features error', 'error']
  ] as const) {
    it(`${name}: no payment parts, the rest of the policy unchanged`, withEnv(FILLED_SALE_CONFIG, () => {
      const el = render(body);
      expect(payments(el)).toBeNull();
      expect(el.textContent).not.toContain('Stripe');
      expect(el.querySelector('[data-test="privacy-purchase-retention"]')).toBeNull();
      expect(el.textContent).toContain('Contabo GmbH');
    }));
  }

  it('billing on but seller config missing: no payment parts (same rule as the terms of sale)', withEnv({ ...FILLED_SALE_CONFIG, sellerName: '' }, () => {
    spyOn(console, 'error');
    const el = render({ aiComment: false, billingEnabled: true });
    expect(payments(el)).toBeNull();
    expect(el.textContent).not.toContain('Stripe');
  }));
});
