import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AiReadinessService, isGrandfathered, isValidSavedPlan, lockedResultView } from './ai-readiness.service';
import { AssessmentResult } from '../models/ai-readiness.model';

const RESULT_KEY = 'ai_readiness_last_result';
const PROGRESS_KEY = 'ai_readiness_roadmap_progress';

function full(extra: Partial<AssessmentResult> = {}): AssessmentResult {
  return {
    professionId: 'onet-1', professionName: 'Accountant', replaceabilityScore: 40, taskExposureScore: 35,
    personalAdjustment: 5, band: 'MODERATE', headline: 'h', readinessScore: 20, readinessLevel: 'Explorer',
    tasks: [], humanEdge: [], aiTools: [], pivotRoles: [],
    generalRoadmap: [{ id: 'g1', title: 'Basics', goal: 'goal', status: 'CURRENT', weeks: 2, items: ['Learn prompts'], resources: [] }],
    professionRoadmap: [{ title: 'Phase A', items: ['Do X'], project: 'Build Y' }],
    totalWeeks: 10, personalNote: null, ...extra
  } as AssessmentResult;
}

describe('AiReadinessService storage for locked results (FP-63)', () => {
  let api: AiReadinessService;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(AiReadinessService);
  });
  afterEach(() => localStorage.clear());

  it('saveLastResult stores only the teaser for a locked result and clears a stale full roadmap', () => {
    api.saveLastResult(full());
    api.saveProgress('onet-1', ['Do X']);
    expect(localStorage.getItem(RESULT_KEY)).toContain('Learn prompts');
    api.saveLastResult(full({ roadmapLocked: true, roadmapTeaser: ['Teaser'] }), { q1: 'a' });
    const raw = localStorage.getItem(RESULT_KEY)!;
    expect(raw).not.toContain('Learn prompts');
    expect(raw).not.toContain('Build Y');
    expect(raw).toContain('Teaser');
    // progress (ticked step texts) is kept, so a paying user who re-takes the test keeps the ticks (QA #8)
    expect(api.loadProgress('onet-1')).toEqual(['Do X']);
  });

  it('loadLastResult strips and rewrites a full roadmap stored with a locked result', () => {
    localStorage.setItem(RESULT_KEY, JSON.stringify(full({ roadmapLocked: true })));
    const r = api.loadLastResult()!;
    expect(r.professionRoadmap).toEqual([]);
    expect(localStorage.getItem(RESULT_KEY)).not.toContain('Learn prompts');
  });

  it('savedPlan ("Continue your plan") is null for a locked result', () => {
    localStorage.setItem(RESULT_KEY, JSON.stringify(full({ roadmapLocked: true })));
    expect(api.savedPlan()).toBeNull();
    localStorage.setItem(RESULT_KEY, JSON.stringify(full()));
    expect(api.savedPlan()).not.toBeNull();
  });

  it('lockedResultView leaves unlocked results unchanged', () => {
    const r = full();
    expect(lockedResultView(r)).toBe(r);
  });

  it('every save writes the billing marker, also with billing off', () => {
    api.saveLastResult(full());
    let saved = JSON.parse(localStorage.getItem(RESULT_KEY)!);
    expect(saved.billingMarker).toBe('v1');
    expect(saved.billingEnabled).toBeFalse();
    api.saveLastResult(full({ billingEnabled: true, roadmapLocked: true }));
    saved = JSON.parse(localStorage.getItem(RESULT_KEY)!);
    expect(saved.billingMarker).toBe('v1');
    expect(saved.billingEnabled).toBeTrue();
    expect(isGrandfathered(saved)).toBeFalse();
  });

  it('grandfathered (pre-billing, unmarked) record: kept as it is, with progress and "Continue your plan"', () => {
    const raw = JSON.stringify(full());
    localStorage.setItem(RESULT_KEY, raw);
    localStorage.setItem(PROGRESS_KEY, JSON.stringify({ 'onet-1': ['Do X'] }));
    const r = api.loadLastResult()!;
    expect(isGrandfathered(r)).toBeTrue();
    expect(r.professionRoadmap.length).toBe(1);
    expect(localStorage.getItem(RESULT_KEY)).toBe(raw);
    expect(api.loadProgress('onet-1')).toEqual(['Do X']);
    expect(api.savedPlan()).toEqual(jasmine.objectContaining({ done: 1, total: 2 }));
  });

  it('a marked stale record is still cleaned', () => {
    localStorage.setItem(RESULT_KEY, JSON.stringify(full({ billingMarker: 'v1', billingEnabled: true })));
    expect(api.loadLastResult()!.professionRoadmap).toEqual([]);
    expect(localStorage.getItem(RESULT_KEY)).not.toContain('Learn prompts');
  });

  it('re-take with the same profession keeps the ticks; another profession clears the old ones', () => {
    api.saveLastResult(full({ billingEnabled: true, roadmapLocked: true }));
    api.saveProgress('onet-1', ['Do X']);
    api.saveLastResult(full({ billingEnabled: true, roadmapLocked: true }));
    expect(api.loadProgress('onet-1')).toEqual(['Do X']);
    api.saveLastResult(full({ professionId: 'onet-2', billingEnabled: true, roadmapLocked: true }));
    expect(api.loadProgress('onet-1')).toEqual([]);
  });

  it('an invalid grandfathered record is ignored (not rendered) but not deleted', () => {
    for (const bad of [
      { ...full(), professionId: '' },
      { ...full(), professionRoadmap: [{ title: 'A', items: [1, 2], project: 'p' }] },
      { ...full(), professionRoadmap: 'nope' },
      { ...full(), generalRoadmap: [{ id: 'g', title: 't', status: 'WEIRD', items: [] }] },
      { ...full(), tasks: null }
    ]) {
      const raw = JSON.stringify(bad);
      localStorage.setItem(RESULT_KEY, raw);
      expect(api.loadLastResult()).withContext(raw).toBeNull();
      expect(api.savedPlan()).toBeNull();
      expect(localStorage.getItem(RESULT_KEY)).toBe(raw);
    }
  });

  it('a valid grandfathered record passes the shape check', () => {
    expect(isValidSavedPlan(full())).toBeTrue();
    expect(isValidSavedPlan(null)).toBeFalse();
  });
});
