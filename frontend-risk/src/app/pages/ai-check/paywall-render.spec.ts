import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AiCheckPageComponent } from './ai-check.page';
import { AssessmentResult } from '../../models/ai-readiness.model';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

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

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    queryParams = {};
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
    localStorage.clear();
    sessionStorage.clear();
  });

  function boot(): HTMLElement {
    fixture = TestBed.createComponent(AiCheckPageComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/professions`).flush([]);
    http.expectOne(`${API}/questions`).flush([]);
    http.expectOne(`${API}/professions/categories`).flush([]);
    http.expectOne(`${API}/features`).flush({ aiComment: false });
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
    const el = show(result({ roadmapLocked: true, generalRoadmap: [], professionRoadmap: [], roadmapTeaser: ['Phase 1: AI basics'] }));
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(el.querySelector('[data-test="roadmap-teaser"]')!.textContent).toContain('Phase 1: AI basics');
    expect(el.textContent).not.toContain('General AI learning path');
  });

  it('loadFullRoadmap renders the paid roadmap from POST /roadmap with the existing plan view', () => {
    const locked = result({ roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(locked);
    fixture.componentInstance.answers = { q1: 'a' };
    fixture.componentInstance.loadFullRoadmap(locked);
    const req = http.expectOne(`${API}/roadmap`);
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    req.flush({ generalRoadmap: result().generalRoadmap, professionRoadmap: [{ title: 'Paid phase', items: ['p'], project: 'pp' }], totalWeeks: 7 });
    fixture.detectChanges();
    expect(el.querySelector('app-paywall-card')).toBeNull();
    expect(el.textContent).toContain('Paid phase');
    expect(fixture.componentInstance.result!.totalWeeks).toBe(7);
  });

  it('a 402 from POST /roadmap keeps the locked state and explains it', () => {
    const locked = result({ roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] });
    const el = show(locked);
    fixture.componentInstance.loadFullRoadmap(locked);
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
    req.flush(result({ roadmapLocked: true, generalRoadmap: [], professionRoadmap: [] }));
    fixture.detectChanges();
    expect(fixture.componentInstance.step).toBe('result');
    expect(fixture.componentInstance.answers).toEqual({ q1: 'a' });
    expect(el.querySelector('app-paywall-card')).not.toBeNull();
    expect(sessionStorage.getItem('ai_readiness_pending_purchase')).toBeNull();
  });
});
