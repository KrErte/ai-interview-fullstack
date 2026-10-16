import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AiReadinessService, lockedResultView } from './ai-readiness.service';
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
    expect(localStorage.getItem(PROGRESS_KEY) ?? '').not.toContain('Do X');
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
});
