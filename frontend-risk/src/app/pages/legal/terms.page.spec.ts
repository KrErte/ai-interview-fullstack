import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TermsPageComponent } from './terms.page';
import { WAIVER_TEXT } from '../../models/billing.model';

describe('TermsPageComponent', () => {
  it('keeps the terms of use, adds the terms of sale with the same waiver text and visible placeholders', () => {
    TestBed.configureTestingModule({ imports: [TermsPageComponent], providers: [provideRouter([])] });
    const f = TestBed.createComponent(TermsPageComponent);
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    const text = el.textContent ?? '';
    expect(text).toContain('What the test is, and what it is not');
    expect(text).toContain('Terms of sale – Personal AI roadmap');
    expect(el.querySelector('[data-test="terms-waiver"]')!.textContent).toContain(WAIVER_TEXT);
    expect(text).toContain('[SELLER NAME]');
    expect(text).not.toMatch(/12-week|saved in your account|saved to your account/i);
  });
});
