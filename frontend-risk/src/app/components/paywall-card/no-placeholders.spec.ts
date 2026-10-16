import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PaywallCardComponent } from './paywall-card.component';
import { AuthService } from '../../services/auth.service';
import { AiCheckPageComponent } from '../../pages/ai-check/ai-check.page';
import { AssessmentResult } from '../../models/ai-readiness.model';

/**
 * No raw "[PLACEHOLDER]" text may reach users on the paywall card or the teaser. /terms and /refund are
 * excluded on purpose: they are FP-65 legal drafts whose [SELLER NAME], [EMAIL], [DATE] … placeholders must
 * stay visible until Olen/legal fill them in.
 */
const PLACEHOLDER = /\[[A-Z]/;

describe('No visible placeholders on the paywall card and teaser (FP-65)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: () => false } },
        { provide: ActivatedRoute, useValue: { snapshot: { fragment: null, queryParamMap: convertToParamMap({}) }, fragment: of(null) } }
      ]
    });
  });

  it('paywall card', () => {
    const f = TestBed.createComponent(PaywallCardComponent);
    f.componentRef.setInput('professionId', 'onet-1');
    f.componentRef.setInput('professionName', 'Accountant');
    f.componentRef.setInput('answers', {});
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).not.toMatch(PLACEHOLDER);
  });

  it('teaser section on the locked result', () => {
    const f = TestBed.createComponent(AiCheckPageComponent);
    const r = {
      professionId: 'onet-1', professionName: 'Accountant', replaceabilityScore: 40, taskExposureScore: 35,
      personalAdjustment: 5, band: 'MODERATE', headline: 'h', readinessScore: 20, readinessLevel: 'Explorer',
      tasks: [], humanEdge: [], aiTools: [], pivotRoles: [], generalRoadmap: [], professionRoadmap: [], totalWeeks: 0,
      personalNote: null, billingEnabled: true, roadmapLocked: true, roadmapTeaser: ['Phase 1: AI basics', 'Phase 2: Automate']
    } as AssessmentResult;
    f.componentInstance.result = r;
    f.componentInstance.step = 'result';
    f.detectChanges();
    const section = (f.nativeElement as HTMLElement).querySelector('#roadmap-paywall')!;
    expect(section).not.toBeNull();
    expect(section.textContent).not.toMatch(PLACEHOLDER);
  });
});
