import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RefundPageComponent } from './refund.page';

describe('RefundPageComponent', () => {
  it('shows the placeholder', () => {
    TestBed.configureTestingModule({ imports: [RefundPageComponent], providers: [provideRouter([])] });
    const f = TestBed.createComponent(RefundPageComponent);
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).toContain('Refund policy: final text coming soon.');
  });
});
