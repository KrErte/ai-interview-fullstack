import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AiCheckPageComponent } from './ai-check.page';
import { AssessmentResult } from '../../models/ai-readiness.model';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';
import { FILLED_SALE_CONFIG } from '../legal/legal-test-config';

const API = `${environment.apiBaseUrl}/api/ai-readiness`;

function result(extra: Partial<AssessmentResult> = {}): AssessmentResult {
  return {
    professionId: 'onet-1', professionName: 'Accountant', replaceabilityScore: 40, taskExposureScore: 35,
    personalAdjustment: 5, band: 'MODERATE', headline: 'h', readinessScore: 20, readinessLevel: 'Explorer',
    tasks: [], humanEdge: [], aiTools: [], pivotRoles: [],
    generalRoadmap: [{ id: 'g1', title: 'Basics', goal: 'goal', status: 'CURRENT', weeks: 2, items: ['Learn prompts'], resources: [] }],
    professionRoadmap: [{ title: 'Phase A', items: ['Do X'], project: 'Build Y' }],
    totalWeeks: 10, personalNote: null, ...extra
  } as AssessmentResult;
}

describe('AiCheckPageComponent paid roadmap rendering (FP-63)', () => {
  let fixture: ComponentFixture<AiCheckPageComponent>;
  let http: HttpTestingController;
  let queryParams: Record<string, string>;
  /** billingEnabled in the /features answer (the card's single source of truth). */
  let serverBilling: boolean | undefined;

  let savedEnv: Record<string, unknown>;
  const env = environment as unknown as Record<string, unknown>;
  beforeEach(() => {
    // most specs run with a complete legal config (sale ready); the "config empty" spec resets it
    savedEnv = Object.fromEntries(Object.keys(FILLED_SALE_CONFIG).map(k => [k, env[k]]));
    Object.assign(env, FILLED_SALE_CONFIG);
    localStorage.clear();
    sessionStorage.clear();
    queryParams = {};
    serverBilling = true;
    TestBed.configureTestingModule({
      imports: [AiCheckPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: () => false } },
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: { fragment: null, queryParamMap: convertToParamMap(queryParams) },
            fragment: of(null), queryParamMap: of(convertToParamMap(queryParams))
          })
        }
      ]
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    Object.assign(env, savedEnv);
    localStorage.clear();
    sessionStorage.clear();
  });

  function boot(): HTMLElement {
    fixture = TestBed.createComponent(AiCheckPageComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/professions`).flush([]);
    http.expectOne(`${API}/questions`).flush([]);
    http.expectOne(`${API}/professions/categories`).flush([]);
    // cached per app session: a second boot in the same spec (a "reload") makes no new request
    http.match(`${API}/features`).forEach(r => r.flush({ aiComment: false, billingEnabled: serverBilling }));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }
  function show(r: AssessmentResult): HTMLElement {
    const el = boot();
    fixture.componentInstance.result = r;
    fixture.componentInstance.step = 'result';
    fixture.detectChanges();
    return el;
  }

  it('billing off (roadmapLocked absent): renders the roadmaps exactly as before, no paywall', () => {
    const el = show(result());
    expect(el.textContent).toContain('General AI learning path');
    expect(el.textContent).toContain('Starter plan: Accountant');
    expect(el.querySelector('app-paywall-card')).toBeNull();
  });

  it('roadmapLocked false behaves like billing off', () => {
    const el = show(result({ roadmapLocked: false }));
    expect(el.textContent).toContain('General AI learning path');
    expect(el.querySelector('app-paywall-card')).toBeNull();
  });

  it('roadmapLocked true: shows the teaser and the price card instead of the roadmaps', () => {
    const el = show(result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [], roadmapTeaser: ['Phase 1: AI basics'] }));
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(el.querySelector('[data-test="roadmap-teaser"]')!.textContent).toContain('Phase 1: AI basics');
    expect(el.textContent).not.toContain('General AI learning path');
  });

  it('loadFullRoadmap renders the paid roadmap from POST /roadmap with the existing plan view', () => {
    const locked = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(locked);
    fixture.componentInstance.answers = { q1: 'a' };
    fixture.componentInstance.loadFullRoadmap(locked);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: true });
    const req = http.expectOne(`${API}/roadmap`);
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    req.flush({ generalRoadmap: result().generalRoadmap, professionRoadmap: [{ title: 'Paid phase', items: ['p'], project: 'pp' }], totalWeeks: 7 });
    fixture.detectChanges();
    expect(el.querySelector('app-paywall-card')).toBeNull();
    expect(el.textContent).toContain('Paid phase');
    expect(el.querySelector('[data-test="payment-confirmed"]')!.textContent).toContain('Payment confirmed');
    expect(fixture.componentInstance.planOf(fixture.componentInstance.result!)!.totalWeeks).toBe(7);
    // the PDF and .ics controls come back only with the entitled roadmap
    expect(el.querySelector('[data-test="pdf"]')).not.toBeNull();
    // the paid roadmap is never persisted
    expect(localStorage.getItem('ai_readiness_last_result') ?? '').not.toContain('Paid phase');
    expect(sessionStorage.getItem('ai_readiness_pending_purchase') ?? '').not.toContain('Paid phase');
  });

  it('locked: no full roadmap in the DOM, no PDF / .ics controls', () => {
    const locked = result({
      billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['Phase 1: AI basics'],
      tasks: [{ name: 'Bookkeeping', share: 50, automationPercent: 80 }]
    });
    const el = show(locked);
    const text = el.textContent ?? '';
    expect(text).not.toContain('General AI learning path');
    expect(text).not.toContain('Starter plan');
    expect(text).not.toContain('Learn prompts');
    expect(text).not.toContain('Do X');
    expect(text).not.toContain('Build Y');
    expect(el.querySelector('[data-test="pdf"]')).toBeNull();
    expect(el.querySelector('[data-test="ics"]')).toBeNull();
    expect(text).not.toContain('Print / save as PDF');
    expect(text).not.toContain('Add a weekly reminder');
    expect(el.querySelector('a[href*="view=saved"]')).toBeNull();
    expect(el.querySelector('app-paywall-card')!.textContent).toContain('Pay €19');
  });

  it('locked result from /assess: the full roadmap is never kept or written to storage, only the teaser', () => {
    queryParams = { resume: 'paywall' };
    sessionStorage.setItem('ai_readiness_pending_purchase', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' } }));
    boot();
    // even if the backend wrongly sent the roadmaps along with roadmapLocked
    http.expectOne(`${API}/assess`).flush(result({ roadmapLocked: true, roadmapTeaser: ['Teaser line'] }));
    fixture.detectChanges();
    expect(fixture.componentInstance.result!.professionRoadmap).toEqual([]);
    expect(fixture.componentInstance.result!.generalRoadmap).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Learn prompts');
    for (const store of [localStorage, sessionStorage]) {
      for (let i = 0; i < store.length; i++) {
        const v = store.getItem(store.key(i)!) ?? '';
        expect(v).not.toContain('Learn prompts');
        expect(v).not.toContain('Build Y');
      }
    }
  });

  it('billingEnabled true never renders the roadmap from /assess, even when roadmapLocked is false', () => {
    const el = show(result({ billingEnabled: true, roadmapLocked: false }));
    expect(el.textContent).not.toContain('General AI learning path');
    expect(el.textContent).not.toContain('Learn prompts');
    expect(el.querySelector('[data-test="pdf"]')).toBeNull();
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
  });

  it('billingEnabled true: the plan renders only from POST /roadmap', () => {
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush({ generalRoadmap: [], professionRoadmap: [{ title: 'Paid phase', items: ['p'], project: 'pp' }], totalWeeks: 3 });
    fixture.detectChanges();
    expect(el.textContent).toContain('Paid phase');
  });

  it('does not unlock when the entitlement endpoint says not paid: no /roadmap request, paywall stays', () => {
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: false });
    http.expectNone(`${API}/roadmap`);
    fixture.detectChanges();
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(el.querySelector('[data-test="payment-confirmed"]')).toBeNull();
    expect(el.querySelector('[data-test="roadmap-error"]')!.textContent).toContain('not unlocked yet');
  });

  const PHASED = [
    { phase: 1, title: 'AI basics', items: ['Learn prompts for invoices', 'Try a summary tool'], project: 'Automate one report' },
    { phase: 2, title: 'Automate routine work' },
    { phase: 3, title: 'Lead AI adoption' }
  ];

  it('phased teaser: phase 1 shows title, all items and the project', () => {
    const el = show(result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [], roadmapTeaser: PHASED }));
    const p1 = el.querySelector('[data-test="teaser-phase-1"]')!;
    expect(p1.textContent).toContain('Phase 1: AI basics');
    expect(p1.textContent).toContain('Learn prompts for invoices');
    expect(p1.textContent).toContain('Try a summary tool');
    expect(p1.querySelector('[data-test="teaser-project"]')!.textContent).toContain('Automate one report');
    expect(p1.querySelector('[data-test="teaser-locked"]')).toBeNull();
  });

  it('phased teaser: phases 2-3 show titles only, marked locked in text, with no items in the DOM', () => {
    const teaser = [PHASED[0], { ...PHASED[1], items: ['Leaked item'], project: 'Leaked project' }, PHASED[2]];
    const el = show(result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [], roadmapTeaser: teaser }));
    for (const n of [2, 3]) {
      const ph = el.querySelector(`[data-test="teaser-phase-${n}"]`)!;
      expect(ph.textContent).toContain(`Phase ${n}:`);
      expect(ph.querySelector('[data-test="teaser-locked"]')!.textContent).toContain('Locked');
      expect(ph.getAttribute('aria-label')).toContain('locked');
      expect(ph.querySelector('[data-test="teaser-items"]')).toBeNull();
      expect(ph.querySelector('[data-test="teaser-project"]')).toBeNull();
    }
    expect(el.textContent).not.toContain('Leaked item');
    expect(el.textContent).not.toContain('Leaked project');
  });

  it('old teaser shapes still render: string, string[] and {title}[]', () => {
    for (const [teaser, expected] of [
      ['Week 1: prompts', 'Week 1: prompts'],
      [['Line A', 'Line B'], 'Line B'],
      [[{ title: 'Titled stage' }], 'Titled stage']
    ] as const) {
      fixture?.destroy();
      const el = show(result({ roadmapLocked: true, generalRoadmap: [], professionRoadmap: [], roadmapTeaser: teaser as never }));
      expect(el.querySelector('[data-test="roadmap-teaser"]')!.textContent).toContain(expected);
      expect(el.querySelector('[data-test="teaser-phase-1"]')).toBeNull();
    }
  });

  it('grandfathered saved plan: rendered with progress, PDF and .ics, no paywall, never re-saved', () => {
    queryParams = { view: 'saved' };
    const raw = JSON.stringify(result({ tasks: [{ name: 'Bookkeeping', share: 50, automationPercent: 80 }] }));
    localStorage.setItem('ai_readiness_last_result', raw);
    localStorage.setItem('ai_readiness_last_answers', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' } }));
    localStorage.setItem('ai_readiness_roadmap_progress', JSON.stringify({ 'onet-1': ['Do X'] }));
    const el = boot();
    http.expectNone(`${API}/assess`);
    fixture.detectChanges();
    expect(fixture.componentInstance.grandfathered).toBeTrue();
    expect(el.textContent).toContain('Starter plan: Accountant');
    expect(el.textContent).toContain('1 / 2 done');
    expect(el.querySelector('[data-test="pdf"]')).not.toBeNull();
    expect(el.querySelector('[data-test="ics"]')).not.toBeNull();
    expect(el.querySelector('app-paywall-card')).toBeNull();
    expect(localStorage.getItem('ai_readiness_last_result')).toBe(raw);
  });

  it('grandfathered: ticks save to ai_readiness_roadmap_progress and survive a reload; Reset hidden; record not re-saved', () => {
    queryParams = { view: 'saved' };
    const raw = JSON.stringify(result());
    localStorage.setItem('ai_readiness_last_result', raw);
    localStorage.setItem('ai_readiness_last_answers', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' }, taskShares: { t: 100 } }));
    let el = boot();
    fixture.detectChanges();
    const box = Array.from(el.querySelectorAll('label')).find(l => l.textContent!.includes('Do X'))!.querySelector('input')!;
    expect(box.checked).toBeFalse();
    box.click();
    fixture.detectChanges();
    expect(JSON.parse(localStorage.getItem('ai_readiness_roadmap_progress')!)['onet-1']).toEqual(['Do X']);
    expect(el.textContent).not.toContain('Reset to the typical week');
    // reload
    fixture.destroy();
    el = boot();
    fixture.detectChanges();
    const again = Array.from(el.querySelectorAll('label')).find(l => l.textContent!.includes('Do X'))!.querySelector('input')!;
    expect(again.checked).toBeTrue();
    expect(el.textContent).toContain('1 / 2 done');
    expect(localStorage.getItem('ai_readiness_last_result')).toBe(raw);
    http.expectNone(`${API}/assess`);
  });

  it('a new locked assessment after a grandfathered plan is gated and overwrites the saved result', () => {
    localStorage.setItem('ai_readiness_last_result', JSON.stringify(result()));
    boot();
    const c = fixture.componentInstance;
    c.professionId = 'onet-1';
    c.answers = {};
    c.submit();
    http.expectOne(`${API}/assess`).flush(result({ billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['T'] }));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(c.grandfathered).toBeFalse();
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(el.textContent).not.toContain('Learn prompts');
    const saved = JSON.parse(localStorage.getItem('ai_readiness_last_result')!);
    expect(saved.billingMarker).toBe('v1');
    expect(saved.professionRoadmap).toEqual([]);
  });

  it('server billing on + legal config empty: teaser and a neutral note, no card, no Pay button, no full roadmap', () => {
    Object.assign(env, savedEnv);
    const el = show(result({ billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['Phase 1: AI basics'] }));
    expect(el.querySelector('[data-test="roadmap-teaser"]')).not.toBeNull();
    expect(el.querySelector('[data-test="roadmap-soon"]')!.textContent).toContain('The full roadmap will be available soon.');
    expect(el.querySelector('app-paywall-card')).toBeNull();
    expect(el.textContent).not.toContain('Pay €19');
    expect(el.textContent).not.toContain('Learn prompts');
    expect(el.textContent).not.toContain('General AI learning path');
  });

  for (const [name, flag] of [['false', false], ['missing', undefined]] as const) {
    it(`/features billingEnabled ${name} + config complete: no card, no Pay button (fail closed)`, () => {
      serverBilling = flag;
      const el = show(result({ billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['Phase 1: AI basics'] }));
      expect(el.querySelector('app-paywall-card')).toBeNull();
      expect(el.querySelector('[data-test="roadmap-soon"]')).not.toBeNull();
      expect(el.textContent).not.toContain('Learn prompts');
    });
  }

  it('/features error + config complete: no card (fail closed), the test still loads', () => {
    fixture = TestBed.createComponent(AiCheckPageComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/professions`).flush([]);
    http.expectOne(`${API}/questions`).flush([]);
    http.expectOne(`${API}/professions/categories`).flush([]);
    http.expectOne(`${API}/features`).flush(null, { status: 500, statusText: 'Server Error' });
    fixture.componentInstance.result = result({ billingEnabled: true, roadmapLocked: true });
    fixture.componentInstance.step = 'result';
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-paywall-card')).toBeNull();
  });

  it('server billing on + legal config complete: the card with the Pay button', () => {
    const el = show(result({ billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['Phase 1: AI basics'] }));
    expect(el.querySelector('[data-test="roadmap-soon"]')).toBeNull();
    expect(el.querySelector('app-paywall-card')!.textContent).toContain('Pay €19');
  });

  it('"Payment confirmed" shows once per tab session, not on every entitled visit', () => {
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const full = { generalRoadmap: [], professionRoadmap: [{ title: 'Paid phase', items: ['p'], project: 'pp' }], totalWeeks: 3 };
    let el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush(full);
    fixture.detectChanges();
    expect(el.querySelector('[data-test="payment-confirmed"]')).not.toBeNull();
    fixture.destroy();
    el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush(full);
    fixture.detectChanges();
    expect(el.textContent).toContain('Paid phase');
    expect(el.querySelector('[data-test="payment-confirmed"]')).toBeNull();
  });

  const ENT_URL = `${environment.apiBaseUrl}/api/billing/entitlement`;
  const PAID_FULL = { generalRoadmap: [], professionRoadmap: [{ title: 'Paid phase', items: ['p'], project: 'pp' }], totalWeeks: 3 };

  it('"Payment confirmed" shows right after a purchase (?paid=1) even later in the session', fakeAsync(() => {
    sessionStorage.setItem('ai_readiness_payment_confirmed_shown', '1');
    queryParams = { paid: '1' };
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    tick(0);
    http.expectOne(ENT_URL).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush(PAID_FULL);
    fixture.detectChanges();
    expect(el.querySelector('[data-test="payment-confirmed"]')).not.toBeNull();
  }));

  it('resume flow after payment polls every 1 s and unlocks only on roadmap:true', fakeAsync(() => {
    queryParams = { paid: '1' };
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    tick(0);
    http.expectOne(ENT_URL).flush({ roadmap: false });
    tick(1000);
    http.expectOne(ENT_URL).flush({ roadmap: false });
    http.expectNone(`${API}/roadmap`);
    tick(1000);
    http.expectOne(ENT_URL).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush(PAID_FULL);
    fixture.detectChanges();
    expect(el.textContent).toContain('Paid phase');
    tick(20000);
    http.expectNone(ENT_URL);
  }));

  it('resume flow: no roadmap:true within 15 s shows "unlocking can take a minute" with a Retry button that polls again', fakeAsync(() => {
    queryParams = { paid: '1' };
    const r = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(r);
    fixture.componentInstance.loadFullRoadmap(r);
    for (let i = 0; i <= 15; i++) {
      tick(i === 0 ? 0 : 1000);
      http.expectOne(ENT_URL).flush({ roadmap: false });
    }
    fixture.detectChanges();
    const pending = el.querySelector('[data-test="payment-pending"]')!;
    expect(pending.textContent).toContain('Payment received, unlocking can take a minute.');
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    const retry = el.querySelector('[data-test="pending-retry"]') as HTMLButtonElement;
    expect(retry.tagName).toBe('BUTTON');
    retry.click();
    tick(0);
    http.expectOne(ENT_URL).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush(PAID_FULL);
    fixture.detectChanges();
    expect(el.querySelector('[data-test="payment-pending"]')).toBeNull();
    expect(el.textContent).toContain('Paid phase');
  }));

  it('cancel return (?canceled=1): neutral note, paywall still offered', () => {
    queryParams = { canceled: '1' };
    const el = show(result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] }));
    expect(el.querySelector('[data-test="payment-cancelled"]')!.textContent).toContain('Payment cancelled. Nothing was charged.');
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
  });

  it('a paying user who re-takes the test keeps their ticks (progress is never cleared on save)', () => {
    localStorage.setItem('ai_readiness_roadmap_progress', JSON.stringify({ 'onet-1': ['p'] }));
    boot();
    const c = fixture.componentInstance;
    c.professionId = 'onet-1';
    c.submit();
    http.expectOne(`${API}/assess`).flush(result({ billingEnabled: true, roadmapLocked: true }));
    expect(JSON.parse(localStorage.getItem('ai_readiness_roadmap_progress')!)['onet-1']).toEqual(['p']);
  });

  it('billingEnabled missing counts as billing off', () => {
    const el = show(result({ billingEnabled: undefined }));
    expect(el.textContent).toContain('General AI learning path');
  });

  it('billing off: the PDF and .ics controls are still shown', () => {
    const el = show(result({ tasks: [{ name: 'Bookkeeping', share: 50, automationPercent: 80 }] }));
    expect(el.querySelector('[data-test="pdf"]')).not.toBeNull();
    expect(el.querySelector('[data-test="ics"]')).not.toBeNull();
  });

  it('a 402 from POST /roadmap keeps the locked state and explains it', () => {
    const locked = result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(locked);
    fixture.componentInstance.loadFullRoadmap(locked);
    http.expectOne(`${environment.apiBaseUrl}/api/billing/entitlement`).flush({ roadmap: true });
    http.expectOne(`${API}/roadmap`).flush({ error: 'payment_required' }, { status: 402, statusText: 'Payment Required' });
    fixture.detectChanges();
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(el.querySelector('[data-test="roadmap-error"]')!.textContent).toContain('not unlocked yet');
  });

  it('?resume=paywall restores the saved answers and shows the paywall again', () => {
    queryParams = { resume: 'paywall' };
    sessionStorage.setItem('ai_readiness_pending_purchase', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' } }));
    const el = boot();
    const req = http.expectOne(`${API}/assess`);
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' }, personalize: false });
    req.flush(result({ billingEnabled: true, roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] }));
    fixture.detectChanges();
    expect(fixture.componentInstance.step).toBe('result');
    expect(fixture.componentInstance.answers).toEqual({ q1: 'a' });
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(sessionStorage.getItem('ai_readiness_pending_purchase')).toBeNull();
  });
});
