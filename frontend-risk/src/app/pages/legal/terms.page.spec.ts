import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TermsPageComponent } from './terms.page';
import { WAIVER_LABEL, WAIVER_TEXT, WAIVER_TEXT_VERSION } from '../../models/billing.model';
import { environment } from '../../../environments/environment';
import { FILLED_SALE_CONFIG, withEnv } from './legal-test-config';

function render(): HTMLElement {
  TestBed.configureTestingModule({ imports: [TermsPageComponent], providers: [provideRouter([])] });
  const f = TestBed.createComponent(TermsPageComponent);
  f.detectChanges();
  return f.nativeElement as HTMLElement;
}

describe('TermsPageComponent', () => {
  it('config empty: keeps the terms of use and hides the sale section', () => {
    const el = render();
    expect(el.textContent).toContain('What the test is, and what it is not');
    expect(el.querySelector('[data-test="terms-of-sale"]')).toBeNull();
  });

  it('billing on but seller config missing: sale section stays hidden', withEnv({ billingEnabled: true }, () => {
    expect(render().querySelector('[data-test="terms-of-sale"]')).toBeNull();
  }));

  it('config filled: renders the sale section with config values and the shared waiver', withEnv(FILLED_SALE_CONFIG, () => {
    const el = render();
    const sale = el.querySelector('[data-test="terms-of-sale"]')!;
    expect(sale.textContent).toContain('Terms of sale – Personal AI roadmap');
    expect(sale.textContent).toContain(environment.sellerName);
    expect(sale.textContent).toContain('EE123456789');
    expect(el.querySelector('[data-test="sale-price"]')!.textContent).toContain('EUR 19 incl. VAT');
    const w = el.querySelector('[data-test="terms-waiver"]')!.textContent!;
    expect(w).toContain(WAIVER_TEXT);
    expect(w).toContain(WAIVER_LABEL);
    expect(w).toContain(WAIVER_TEXT_VERSION);
    expect(el.textContent).toContain('What the test is, and what it is not');
    expect(el.querySelector('[data-test="withdrawal-kept"]')!.textContent!.trim())
      .toBe('You keep your right of withdrawal until we have sent you the confirmation of your consent.');
  }));
});
