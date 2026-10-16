import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { PAYWALL_COPY, PAYWALL_RETURN_URL, PaywallCardComponent } from './paywall-card.component';
import { AnalyticsService } from '../../services/analytics.service';
import { AuthService } from '../../services/auth.service';
import { BrowserRedirectService } from '../../services/browser-redirect.service';
import { AiReadinessService } from '../../services/ai-readiness.service';
import { environment } from '../../../environments/environment';
import { WAIVER_TEXT, WAIVER_TEXT_VERSION } from '../../models/billing.model';

const ENT = `${environment.apiBaseUrl}/api/billing/entitlement`;
const CHECKOUT = `${environment.apiBaseUrl}/api/billing/checkout`;

type IOCallback = (entries: Array<{ isIntersecting: boolean }>) => void;

describe('PaywallCardComponent', () => {
  let fixture: ComponentFixture<PaywallCardComponent>;
  let http: HttpTestingController;
  let track: jasmine.Spy;
  let assign: jasmine.Spy;
  let loggedIn: boolean;
  let ioCallbacks: IOCallback[];
  const w = window as unknown as { IntersectionObserver: unknown };
  const realIO = w.IntersectionObserver;

  beforeEach(() => {
    sessionStorage.clear();
    loggedIn = true;
    ioCallbacks = [];
    w.IntersectionObserver = class {
      constructor(cb: IOCallback) { ioCallbacks.push(cb); }
      observe() {}
      disconnect() {}
    };
    track = jasmine.createSpy('track');
    assign = jasmine.createSpy('assign');
    TestBed.configureTestingModule({
      imports: [PaywallCardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AnalyticsService, useValue: { track } },
        { provide: BrowserRedirectService, useValue: { assign } },
        { provide: AuthService, useValue: { isLoggedIn: () => loggedIn } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    w.IntersectionObserver = realIO;
    sessionStorage.clear();
  });

  function create(entitled: boolean | null = false): HTMLElement {
    fixture = TestBed.createComponent(PaywallCardComponent);
    fixture.componentRef.setInput('professionId', 'onet-1');
    fixture.componentRef.setInput('answers', { q1: 'a' });
    fixture.detectChanges();
    if (loggedIn && entitled !== null) {
      http.expectOne(ENT).flush({ roadmap: entitled });
      fixture.detectChanges();
    }
    return fixture.nativeElement as HTMLElement;
  }
  const q = (el: HTMLElement, t: string) => el.querySelector(`[data-test="${t}"]`) as HTMLElement | null;
  function tick(el: HTMLElement): void {
    const box = q(el, 'waiver') as HTMLInputElement;
    box.click();
    fixture.detectChanges();
  }
  const events = (name: string) => track.calls.allArgs().filter(a => a[0] === name).length;

  it('FP-65 copy: button text is exactly "Pay €19", waiver is the one versioned text, no unbuilt features', () => {
    const el = create();
    expect(q(el, 'buy')!.textContent!.trim()).toBe('Pay €19');
    expect(PAYWALL_COPY.waiver).toBe(WAIVER_TEXT);
    expect(el.querySelector('label[for="paywall-waiver"]')!.textContent!.trim()).toBe(WAIVER_TEXT);
    expect(q(el, 'waiver')!.getAttribute('data-waiver-version')).toBe(WAIVER_TEXT_VERSION);
    expect(WAIVER_TEXT_VERSION).toBe('waiver-v1-2026-10-16');
    expect((q(el, 'waiver') as HTMLInputElement).checked).toBeFalse();
    expect(el.textContent).not.toMatch(/12-week|12 weeks|saved to your account/i);
  });

  it('shows title, bullets, waiver label and links; buy is disabled until the box is ticked', () => {
    const el = create();
    expect(el.textContent).toContain(PAYWALL_COPY.title);
    expect(el.querySelectorAll('li').length).toBe(PAYWALL_COPY.bullets.length);
    const label = el.querySelector('label[for="paywall-waiver"]')!;
    expect(label.textContent).toContain(PAYWALL_COPY.waiver);
    const hrefs = Array.from(el.querySelectorAll('a')).map(a => a.getAttribute('href'));
    expect(hrefs).toEqual(['/terms', '/refund', '/privacy']);
    const buy = q(el, 'buy') as HTMLButtonElement;
    expect(buy.tagName).toBe('BUTTON');
    expect(buy.disabled).toBeTrue();
    tick(el);
    expect(buy.disabled).toBeFalse();
  });

  it('click: tracks checkout_start before the POST, shows loading, redirects to the url', () => {
    const el = create();
    tick(el);
    q(el, 'buy')!.click();
    fixture.detectChanges();
    expect(events('checkout_start')).toBe(1);
    const req = http.expectOne(CHECKOUT);
    expect(req.request.body).toEqual({ product: 'roadmap', withdrawalWaiverAccepted: true, withdrawalWaiverVersion: WAIVER_TEXT_VERSION });
    expect(q(el, 'buy')!.textContent).toContain(PAYWALL_COPY.busy);
    expect((q(el, 'buy') as HTMLButtonElement).disabled).toBeTrue();
    req.flush({ url: 'https://pay.example/s/1' });
    expect(assign).toHaveBeenCalledOnceWith('https://pay.example/s/1');
  });

  it('409: refreshes the entitlement and shows View your roadmap', () => {
    const el = create();
    tick(el);
    q(el, 'buy')!.click();
    http.expectOne(CHECKOUT).flush(null, { status: 409, statusText: 'Conflict' });
    http.expectOne(ENT).flush({ roadmap: true });
    fixture.detectChanges();
    expect(q(el, 'view-roadmap')!.textContent).toContain(PAYWALL_COPY.view);
    expect(q(el, 'buy')).toBeNull();
    expect(assign).not.toHaveBeenCalled();
  });

  it('400 waiver_required: shows the checkbox error and unticks it', () => {
    const el = create();
    tick(el);
    q(el, 'buy')!.click();
    http.expectOne(CHECKOUT).flush({ error: 'waiver_required' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(q(el, 'waiver-error')!.textContent).toContain(PAYWALL_COPY.waiverError);
    expect((q(el, 'waiver') as HTMLInputElement).checked).toBeFalse();
    expect((q(el, 'buy') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('500 / network: human-readable error with a working retry', () => {
    const el = create();
    tick(el);
    q(el, 'buy')!.click();
    http.expectOne(CHECKOUT).flush({ error: 'boom' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(q(el, 'error')!.textContent).toContain(PAYWALL_COPY.error);
    q(el, 'retry')!.click();
    http.expectOne(CHECKOUT).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(q(el, 'error')).not.toBeNull();
    q(el, 'retry')!.click();
    http.expectOne(CHECKOUT).flush({ url: 'https://pay.example/s/2' });
    expect(assign).toHaveBeenCalledOnceWith('https://pay.example/s/2');
  });

  it('entitled: shows View your roadmap instead of the paywall and emits on click; no paywall_view', () => {
    const el = create(true);
    let viewed = 0;
    fixture.componentInstance.viewRoadmap.subscribe(() => viewed++);
    expect(el.textContent).not.toContain(PAYWALL_COPY.title);
    q(el, 'view-roadmap')!.click();
    expect(viewed).toBe(1);
    expect(ioCallbacks.length).toBe(0);
    expect(events('paywall_view')).toBe(0);
  });

  it('paywall_view fires once when the card becomes visible, not on change detection', () => {
    const el = create();
    expect(events('paywall_view')).toBe(0);
    ioCallbacks[0]([{ isIntersecting: false }]);
    expect(events('paywall_view')).toBe(0);
    ioCallbacks[0]([{ isIntersecting: true }]);
    ioCallbacks[0]([{ isIntersecting: true }]);
    tick(el);
    fixture.detectChanges();
    fixture.detectChanges();
    expect(events('paywall_view')).toBe(1);
  });

  it('paywall_view falls back to first render without IntersectionObserver', () => {
    w.IntersectionObserver = undefined;
    create();
    fixture.detectChanges();
    expect(events('paywall_view')).toBe(1);
  });

  it('logged out: no entitlement call; buy saves the answers and goes to login with the return url', () => {
    loggedIn = false;
    const el = create(null);
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate').and.resolveTo(true);
    expect(q(el, 'buy')!.textContent).toContain(PAYWALL_COPY.buyLoggedOut);
    tick(el);
    q(el, 'buy')!.click();
    expect(events('checkout_start')).toBe(1);
    expect(nav).toHaveBeenCalledOnceWith(['/login'], { queryParams: { returnUrl: PAYWALL_RETURN_URL } });
    expect(TestBed.inject(AiReadinessService).loadPendingPurchase()).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    http.expectNone(CHECKOUT);
  });
});
