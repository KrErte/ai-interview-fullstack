import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { RefundPageComponent } from './refund.page';
import { FILLED_SALE_CONFIG, withSale } from './legal-test-config';

function render(): HTMLElement {
  TestBed.configureTestingModule({ imports: [RefundPageComponent], providers: [provideRouter([]), provideHttpClient()] });
  const f = TestBed.createComponent(RefundPageComponent);
  f.detectChanges();
  return f.nativeElement as HTMLElement;
}

describe('RefundPageComponent', () => {
  it('config empty: no sale-specific content, says nothing is sold yet', () => {
    const el = render();
    expect(el.querySelector('[data-test="refund-sale"]')).toBeNull();
    expect(el.querySelector('[data-test="refund-no-sale"]')!.textContent!.trim())
      .toBe('careerrisk.ee does not sell anything yet. The test is free.');
  });

  it('config filled: round-3 refund content with the seller email', withSale(FILLED_SALE_CONFIG, true, () => {
    const el = render();
    expect(el.querySelector('[data-test="refund-sale"]')!.textContent).toContain('sales@example.test');
    expect(el.textContent).toContain('Last updated: 16 October 2026');
  }));
});
