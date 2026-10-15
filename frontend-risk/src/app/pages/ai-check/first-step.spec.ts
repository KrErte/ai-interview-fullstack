import { firstStep } from './ai-check.page';
import { AssessmentResult, TaskExposure } from '../../models/ai-readiness.model';

function result(tasks: TaskExposure[], withRoadmap = true): AssessmentResult {
  return {
    professionId: 'p',
    professionName: 'P',
    replaceabilityScore: 50,
    taskExposureScore: 50,
    personalAdjustment: 0,
    band: 'HIGH',
    headline: '',
    readinessScore: 0,
    readinessLevel: '',
    tasks,
    humanEdge: [],
    aiTools: ['Tool A', 'Tool B'],
    pivotRoles: [],
    generalRoadmap: [],
    professionRoadmap: withRoadmap ? [{ title: 'Phase', items: ['First item', 'Second'], project: 'First project' }] : [],
    totalWeeks: 0,
    personalNote: null
  };
}

describe('firstStep', () => {
  it('picks the task with the most automatable time', () => {
    const step = firstStep(result([
      { name: 'small but very automatable', share: 10, automationPercent: 90 },
      { name: 'big and automatable', share: 40, automationPercent: 60 },
      { name: 'big but hands-on', share: 50, automationPercent: 10 }
    ]));
    expect(step?.task?.name).toBe('big and automatable');
    expect(step?.tool).toBe('Tool A');
    expect(step?.project).toBe('First project');
  });

  it('suggests no task when AI cannot do much of any of them', () => {
    const step = firstStep(result([{ name: 'cook', share: 60, automationPercent: 12 }]));
    expect(step?.task).toBeNull();
    expect(step?.firstItem).toBe('First item');
  });

  it('is null without tasks or a roadmap', () => {
    expect(firstStep(result([], false))).toBeNull();
  });
});
