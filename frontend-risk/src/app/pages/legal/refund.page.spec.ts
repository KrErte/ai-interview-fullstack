import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RefundPageComponent } from './refund.page';
import { FEATURES_URL, FILLED_SALE_CONFIG, withEnv } from './legal-test-config';

function render(body: unknown): HTMLElement {
  TestBed.configureTestingModule({
    imports: [RefundPageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
  });
  const f = TestBed.createComponent(RefundPageComponent);
  f.detectChanges();
  const req = TestBed.inject(HttpTestingController).expectOne(FEATURES_URL);
  if (body === 'error') req.flush(null, { status: 500, statusText: 'Server Error' }); else req.flush(body as object);
  f.detectChanges();
  return f.nativeElement as HTMLElement;
}

describe('RefundPageComponent', () => {
  it('billingEnabled true + config filled: round-3 refund content with the seller email', withEnv(FILLED_SALE_CONFIG, () => {
    const el = render({ aiComment: false, billingEnabled: true });
    expect(el.querySelector('[data-test="refund-sale"]')!.textContent).toContain('sales@example.test');
    expect(el.textContent).toContain('Last updated: 16 October 2026');
  }));

  for (const [name, body] of [
    ['false', { aiComment: false, billingEnabled: false }],
    ['missing', { aiComment: false }],
    ['an error', 'error']
  ] as const) {
    it(`billingEnabled ${name}: no sale content, says nothing is sold yet`, withEnv(FILLED_SALE_CONFIG, () => {
      const el = render(body);
      expect(el.querySelector('[data-test="refund-sale"]')).toBeNull();
      expect(el.querySelector('[data-test="refund-no-sale"]')!.textContent!.trim())
        .toBe('careerrisk.ee does not sell anything yet. The test is free.');
    }));
  }
});
