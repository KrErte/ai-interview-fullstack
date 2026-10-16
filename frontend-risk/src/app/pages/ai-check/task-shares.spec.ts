import { bandFor, previewResult, rescaleShares } from './task-shares';
import { AssessmentResult } from '../../models/ai-readiness.model';

const total = (s: Record<string, number>) => Object.values(s).reduce((a, b) => a + b, 0);

describe('rescaleShares', () => {
  it('sets the moved share and scales the others to keep 100', () => {
    const s = rescaleShares({ a: 50, b: 30, c: 20 }, 'a', 80);
    expect(s['a']).toBe(80);
    expect(s['b']).toBe(12);
    expect(s['c']).toBe(8);
    expect(total(s)).toBe(100);
  });

  it('gives the rounding remainder to the largest other share', () => {
    const s = rescaleShares({ a: 34, b: 33, c: 33 }, 'a', 0);
    expect(total(s)).toBe(100);
    expect(s['b'] + s['c']).toBe(100);
    expect(Object.values(s).every(Number.isInteger)).toBeTrue();
  });

  it('spreads the rest evenly when the others are all zero', () => {
    const s = rescaleShares({ a: 100, b: 0, c: 0 }, 'a', 40);
    expect(s).toEqual({ a: 40, b: 30, c: 30 });
  });

  it('clamps and rounds the moved value', () => {
    expect(rescaleShares({ a: 50, b: 50 }, 'a', 140)).toEqual({ a: 100, b: 0 });
    expect(rescaleShares({ a: 50, b: 50 }, 'a', -5)).toEqual({ a: 0, b: 100 });
    expect(rescaleShares({ a: 50, b: 50 }, 'a', 33.6)).toEqual({ a: 34, b: 66 });
  });

  it('keeps every total at 100 for random moves', () => {
    let s: Record<string, number> = { a: 18, b: 11, c: 10, d: 9, e: 8, f: 44 };
    for (let i = 0; i < 200; i++) {
      const name = Object.keys(s)[i % 6];
      s = rescaleShares(s, name, (i * 37) % 101);
      expect(total(s)).toBe(100);
      expect(Object.values(s).every(v => Number.isInteger(v) && v >= 0 && v <= 100)).toBeTrue();
    }
  });

  it('leaves a single task or an unknown name unchanged', () => {
    expect(rescaleShares({ a: 100 }, 'a', 20)).toEqual({ a: 100 });
    expect(rescaleShares({ a: 60, b: 40 }, 'zzz', 20)).toEqual({ a: 60, b: 40 });
  });
});

describe('previewResult', () => {
  const base: AssessmentResult = {
    professionId: 'p', professionName: 'P', replaceabilityScore: 45, taskExposureScore: 50,
    personalAdjustment: -5, band: 'MODERATE', headline: '', readinessScore: 20, readinessLevel: '',
    tasks: [
      { name: 'write', share: 50, automationPercent: 80 },
      { name: 'meet', share: 50, automationPercent: 20 }
    ],
    humanEdge: [], aiTools: [], pivotRoles: [], generalRoadmap: [], professionRoadmap: [], totalWeeks: 0,
    personalNote: 'note'
  };

  it('recalculates exposure, keeps the adjustment and moves the band', () => {
    const p = previewResult(base, { write: 100, meet: 0 });
    expect(p.taskExposureScore).toBe(80);
    expect(p.personalAdjustment).toBe(-5);
    expect(p.replaceabilityScore).toBe(75);
    expect(p.band).toBe('VERY_HIGH');
    expect(p.readinessScore).toBe(20);
    expect(p.personalNote).toBe('note');
  });

  it('uses the same band cut-offs as the server', () => {
    expect(bandFor(29)).toBe('LOW');
    expect(bandFor(30)).toBe('MODERATE');
    expect(bandFor(50)).toBe('HIGH');
    expect(bandFor(70)).toBe('VERY_HIGH');
  });
});
