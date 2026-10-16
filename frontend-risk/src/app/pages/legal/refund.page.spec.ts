import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RefundPageComponent } from './refund.page';

describe('RefundPageComponent', () => {
  it('shows the FP-65 draft with its placeholders visible and no unbuilt features', () => {
    TestBed.configureTestingModule({ imports: [RefundPageComponent], providers: [provideRouter([])] });
    const f = TestBed.createComponent(RefundPageComponent);
    f.detectChanges();
    const text = (f.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Refunds and withdrawal');
    expect(text).toContain('[EMAIL]');
    expect(text).toContain('[DATE]');
    expect(text).not.toMatch(/12-week|saved to your account|saved in your account/i);
  });
});
