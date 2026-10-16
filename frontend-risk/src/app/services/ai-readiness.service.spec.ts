import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AiReadinessService } from './ai-readiness.service';
import { AssessmentResult } from '../models/ai-readiness.model';

function result(professionId: string): AssessmentResult {
  return {
    professionId,
    professionName: 'Test',
    replaceabilityScore: 40,
    taskExposureScore: 35,
    personalAdjustment: 5,
    band: 'MODERATE',
    headline: '',
    readinessScore: 20,
    readinessLevel: 'Explorer',
    tasks: [],
    humanEdge: [],
    aiTools: [],
    pivotRoles: [],
    generalRoadmap: [],
    professionRoadmap: [],
    totalWeeks: 10,
    personalNote: null
  };
}

describe('AiReadinessService storage', () => {
  let service: AiReadinessService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AiReadinessService);
  });

  afterEach(() => localStorage.clear());

  it('saves the answers with the result so it can be recalculated', () => {
    service.saveLastResult(result('cook'), { q1: 'a' });
    expect(service.loadLastResult()?.professionId).toBe('cook');
    expect(service.loadLastAnswers()).toEqual({ professionId: 'cook', answers: { q1: 'a' } });
  });

  it('has no answers for a result saved without them', () => {
    localStorage.setItem('ai_readiness_last_result', JSON.stringify(result('cook')));
    expect(service.loadLastResult()).not.toBeNull();
    expect(service.loadLastAnswers()).toBeNull();
  });

  it('ignores broken saved answers', () => {
    localStorage.setItem('ai_readiness_last_answers', '{"answers":{}}');
    expect(service.loadLastAnswers()).toBeNull();
    localStorage.setItem('ai_readiness_last_answers', 'not json');
    expect(service.loadLastAnswers()).toBeNull();
  });

  it('clears the answers together with the result', () => {
    service.saveLastResult(result('cook'), { q1: 'a' });
    service.clearLastResult();
    expect(service.loadLastResult()).toBeNull();
    expect(service.loadLastAnswers()).toBeNull();
  });

  it('keeps roadmap progress per profession', () => {
    service.saveProgress('cook', ['step a', 'step b']);
    service.saveProgress('nurse', ['step c']);
    expect(service.loadProgress('cook')).toEqual(['step a', 'step b']);
    expect(service.loadProgress('nurse')).toEqual(['step c']);
    expect(service.loadProgress('pilot')).toEqual([]);
  });

  it('drops a profession when all its steps are unticked', () => {
    service.saveProgress('cook', ['step a']);
    service.saveProgress('cook', []);
    expect(localStorage.getItem('ai_readiness_roadmap_progress')).toBe('{}');
  });

  it('summarises the saved plan for the home page', () => {
    const saved = {
      ...result('cook'),
      professionName: 'Cook',
      professionRoadmap: [
        { title: 'One', items: ['a', 'b'], project: 'p1' },
        { title: 'Two', items: ['c'], project: 'p2' }
      ]
    };
    service.saveLastResult(saved);
    // 'gone' was reworded out of the catalog and must not count
    service.saveProgress('cook', ['a', 'p1', 'gone']);
    expect(service.savedPlan()).toEqual({ professionName: 'Cook', score: 40, done: 2, total: 5 });
  });

  it('has no saved plan without a saved result', () => {
    expect(service.savedPlan()).toBeNull();
  });

  it('keeps "already done with AI" marks per profession and clears them', () => {
    service.saveAiMarks('cook', ['plan menus']);
    service.saveAiMarks('nurse', ['write notes']);
    expect(service.loadAiMarks('cook')).toEqual(['plan menus']);
    service.saveAiMarks('cook', []);
    expect(service.loadAiMarks('cook')).toEqual([]);
    expect(service.loadAiMarks('nurse')).toEqual(['write notes']);
  });

  it('starts empty when stored progress is not an object', () => {
    localStorage.setItem('ai_readiness_roadmap_progress', '["oops"]');
    expect(service.loadProgress('cook')).toEqual([]);
  });
});
