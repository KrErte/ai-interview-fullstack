import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PaywallCardComponent } from './paywall-card.component';
import { AuthService } from '../../services/auth.service';
import { AiCheckPageComponent } from '../../pages/ai-check/ai-check.page';
import { AssessmentResult } from '../../models/ai-readiness.model';
import { TermsPageComponent } from '../../pages/legal/terms.page';
import { RefundPageComponent } from '../../pages/legal/refund.page';
import { FILLED_SALE_CONFIG, withEnv } from '../../pages/legal/legal-test-config';

/**
 * No raw "[PLACEHOLDER]" text, "re-take", "VAT TBD" or "PLANNED" may reach users on the paywall card, the teaser,
 * /terms or /refund, with the seller/VAT config empty and filled.
 */
const PLACEHOLDER = /\[[A-Z]/;
const BANNED = /re-take|VAT TBD|PLANNED/i;

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
    expect((f.nativeElement as HTMLElement).textContent).not.toMatch(BANNED);
  });

  it('teaser section on the locked result', () => {
    const f = TestBed.createComponent(AiCheckPageComponent);
    const r = {
      professionId: 'onet-1', professionName: 'Accountant', replaceabilityScore: 40, taskExposureScore: 35,
      personalAdjustment: 5, band: 'MODERATE', headline: 'h', readinessScore: 20, readinessLevel: 'Explorer',
      tasks: [], humanEdge: [], aiTools: [], pivotRoles: [], generalRoadmap: [], professionRoadmap: [], totalWeeks: 0,
      personalNote: null, billingEnabled: true, roadmapLocked: true, roadmapTeaser: [{ phase: 1, title: 'AI basics', items: ['Learn prompts'], project: 'Build a report' }, { phase: 2, title: 'Automate' }, { phase: 3, title: 'Lead' }]
    } as AssessmentResult;
    f.componentInstance.result = r;
    f.componentInstance.step = 'result';
    f.detectChanges();
    const section = (f.nativeElement as HTMLElement).querySelector('#roadmap-paywall')!;
    expect(section).not.toBeNull();
    expect(section.textContent).not.toMatch(PLACEHOLDER);
    expect(section.textContent).not.toMatch(BANNED);
  });

  for (const [name, values] of [['config empty', {}], ['config filled', FILLED_SALE_CONFIG]] as const) {
    for (const page of [TermsPageComponent, RefundPageComponent]) {
      it(`${page.name} with ${name}`, withEnv(values, () => {
        const f = TestBed.createComponent(page as never);
        (f as { detectChanges(): void }).detectChanges();
        const text = ((f as { nativeElement: HTMLElement }).nativeElement).textContent ?? '';
        expect(text.length).toBeGreaterThan(50);
        expect(text).not.toMatch(PLACEHOLDER);
        expect(text).not.toMatch(BANNED);
      }));
    }
  }
});
