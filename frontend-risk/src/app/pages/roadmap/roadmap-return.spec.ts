import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RoadmapPageComponent } from './roadmap.page';
import { environment } from '../../../environments/environment';

const ENT = `${environment.apiBaseUrl}/api/billing/entitlement`;
const ROADMAP = `${environment.apiBaseUrl}/api/ai-readiness/roadmap`;
const FULL = { generalRoadmap: [], professionRoadmap: [], totalWeeks: 3 };

describe('RoadmapPageComponent checkout return (?paid=1, FP-64)', () => {
  let http: HttpTestingController;
  let params: Record<string, string>;
  let umami: jasmine.Spy;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    params = { paid: '1', session_id: 'cs_test_123' };
    umami = jasmine.createSpy('track');
    (window as unknown as { umami?: unknown }).umami = { track: umami };
    TestBed.configureTestingModule({
      imports: [RoadmapPageComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useFactory: () => ({ snapshot: { queryParamMap: convertToParamMap(params) }, queryParamMap: of(convertToParamMap(params)) }) }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    localStorage.setItem('ai_readiness_last_answers', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' } }));
  });
  afterEach(() => {
    delete (window as unknown as { umami?: unknown }).umami;
    sessionStorage.clear();
    localStorage.clear();
  });

  function render() {
    const f = TestBed.createComponent(RoadmapPageComponent);
    f.detectChanges();
    return { f, el: f.nativeElement as HTMLElement };
  }
  const q = (el: HTMLElement, t: string) => el.querySelector(`[data-test="${t}"]`);

  it('polls every 1 s; unlocks only on roadmap:true, then loads the roadmap and shows "Payment confirmed"', fakeAsync(() => {
    const { f, el } = render();
    expect(q(el, 'confirming')!.textContent).toContain('Confirming your payment');
    tick(0);
    http.expectOne(ENT).flush({ roadmap: false });
    tick(1000);
    http.expectOne(ENT).flush({ roadmap: false });
    http.expectNone(ROADMAP);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')).toBeNull();
    tick(1000);
    http.expectOne(ENT).flush({ roadmap: true });
    const req = http.expectOne(ROADMAP);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    req.flush(FULL);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')!.textContent).toContain('Payment confirmed');
    expect(q(el, 'view')!.getAttribute('href')).toBe('/ai-test?resume=roadmap');
    tick(20000);
    http.expectNone(ENT);
  }));

  it('failed entitlement checks count as "not yet", not as an unlock', fakeAsync(() => {
    const { f, el } = render();
    tick(0);
    http.expectOne(ENT).flush(null, { status: 500, statusText: 'x' });
    tick(1000);
    http.expectOne(ENT).flush({});
    http.expectNone(ROADMAP);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')).toBeNull();
    f.destroy();
  }));

  it('15 s without roadmap:true: neutral "no confirmation yet" note + Retry button that polls again', fakeAsync(() => {
    const { f, el } = render();
    for (let i = 0; i <= 15; i++) {
      tick(i === 0 ? 0 : 1000);
      http.expectOne(ENT).flush({ roadmap: false });
    }
    tick(5000);
    http.expectNone(ENT);
    f.detectChanges();
    expect(q(el, 'pending')!.querySelector('[data-test="pending-text"]')!.textContent!.trim()).toBe(
      "We haven't received a payment confirmation yet. If you completed the payment, this can take a moment, so press Retry. If the problem continues, contact us.");
    // 'Payment received' only after roadmap:true; never after a timeout
    expect(el.textContent).not.toMatch(/payment received|won't be charged/i);
    expect(umami).not.toHaveBeenCalled();
    const retry = q(el, 'retry') as HTMLButtonElement;
    expect(retry.tagName).toBe('BUTTON');
    retry.click();
    tick(0);
    http.expectOne(ENT).flush({ roadmap: true });
    http.expectOne(ROADMAP).flush(FULL);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')).not.toBeNull();
  }));

  it('session_id never unlocks: with a session_id but no roadmap:true there is no roadmap request and no confirmation', fakeAsync(() => {
    params = { paid: '1', session_id: 'cs_live_looks_valid' };
    const { f, el } = render();
    for (let i = 0; i <= 15; i++) {
      tick(i === 0 ? 0 : 1000);
      const req = http.expectOne(ENT);
      // the entitlement check does not carry the session id either
      expect(req.request.urlWithParams).toBe(ENT);
      req.flush({ roadmap: false });
    }
    http.expectNone(ROADMAP);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')).toBeNull();
    expect(q(el, 'pending')).not.toBeNull();
    expect(el.textContent).not.toMatch(/payment received/i);
    expect(umami).not.toHaveBeenCalled();
  }));

  it('stops polling on destroy', fakeAsync(() => {
    const { f } = render();
    tick(0);
    http.expectOne(ENT).flush({ roadmap: false });
    f.destroy();
    tick(15000);
    http.expectNone(ENT);
  }));

  it('purchase event once per checkout session id (deduped in sessionStorage)', fakeAsync(() => {
    let { f } = render();
    tick(0);
    http.expectOne(ENT).flush({ roadmap: true });
    http.expectOne(ROADMAP).flush(FULL);
    expect(umami).toHaveBeenCalledOnceWith('purchase', { product: 'roadmap' });
    f.destroy();
    ({ f } = render());
    tick(0);
    http.expectOne(ENT).flush({ roadmap: true });
    http.expectOne(ROADMAP).flush(FULL);
    expect(umami).toHaveBeenCalledTimes(1);
    expect(sessionStorage.getItem('fp_purchase_tracked:cs_test_123')).toBe('1');
  }));

  it('without session_id a fallback key is used; without Umami nothing breaks', fakeAsync(() => {
    params = { paid: '1' };
    delete (window as unknown as { umami?: unknown }).umami;
    const { f, el } = render();
    tick(0);
    http.expectOne(ENT).flush({ roadmap: true });
    http.expectOne(ROADMAP).flush(FULL);
    f.detectChanges();
    expect(q(el, 'payment-confirmed')).not.toBeNull();
    expect(sessionStorage.getItem('fp_purchase_tracked:no-session-id')).toBeNull();
  }));
});
