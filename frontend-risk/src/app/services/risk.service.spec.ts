import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  RiskService,
  toFiniteNumber,
  clampPercent,
  buildGaps,
  MISSING_SKILL_PREFIX
} from './risk.service';
import { JobMatchResult, RiskAssessment } from '../models/risk.model';
import { environment } from '../../environments/environment';

describe('toFiniteNumber', () => {
  it('returns finite numbers unchanged (including 0 and negatives)', () => {
    expect(toFiniteNumber(0)).toBe(0);
    expect(toFiniteNumber(42)).toBe(42);
    expect(toFiniteNumber(0.85)).toBe(0.85);
    expect(toFiniteNumber(-5)).toBe(-5);
  });

  it('parses numeric strings, trimming surrounding whitespace', () => {
    expect(toFiniteNumber('80')).toBe(80);
    expect(toFiniteNumber(' 0.85 ')).toBe(0.85);
    expect(toFiniteNumber('\t42\n')).toBe(42);
    expect(toFiniteNumber('-0.2')).toBe(-0.2);
  });

  it("treats '0' as a real zero, not as missing", () => {
    expect(toFiniteNumber('0')).toBe(0);
    expect(toFiniteNumber(' 0 ')).toBe(0);
  });

  it('treats empty and whitespace-only strings as missing (Number("") would be 0)', () => {
    expect(toFiniteNumber('')).toBeNull();
    expect(toFiniteNumber('   ')).toBeNull();
    expect(toFiniteNumber('\n\t')).toBeNull();
  });

  it('treats non-numeric strings as missing', () => {
    expect(toFiniteNumber('abc')).toBeNull();
    expect(toFiniteNumber('80%')).toBeNull();
    expect(toFiniteNumber('Infinity')).toBeNull();
  });

  it('treats null, undefined, NaN and +/-Infinity as missing', () => {
    expect(toFiniteNumber(null)).toBeNull();
    expect(toFiniteNumber(undefined)).toBeNull();
    expect(toFiniteNumber(NaN)).toBeNull();
    expect(toFiniteNumber(Infinity)).toBeNull();
    expect(toFiniteNumber(-Infinity)).toBeNull();
  });

  it('treats booleans and objects as missing', () => {
    expect(toFiniteNumber(true)).toBeNull();
    expect(toFiniteNumber(false)).toBeNull();
    expect(toFiniteNumber({})).toBeNull();
    expect(toFiniteNumber([1])).toBeNull();
  });
});

describe('clampPercent', () => {
  it('clamps below 0 to 0', () => {
    expect(clampPercent(-5)).toBe(0);
    expect(clampPercent(-0.2)).toBe(0);
  });

  it('clamps above 100 to 100', () => {
    expect(clampPercent(150)).toBe(100);
    expect(clampPercent(100.4)).toBe(100);
  });

  it('leaves values inside 0-100 unchanged', () => {
    expect(clampPercent(0)).toBe(0);
    expect(clampPercent(55)).toBe(55);
    expect(clampPercent(100)).toBe(100);
  });
});

describe('buildGaps', () => {
  it('returns an empty list when every source is empty, null or missing', () => {
    expect(buildGaps({})).toEqual([]);
    expect(buildGaps({ weaknesses: [], gaps: [], missingSkills: [] })).toEqual([]);
    expect(buildGaps({ weaknesses: undefined, gaps: null, missingSkills: null })).toEqual([]);
    expect(buildGaps(null)).toEqual([]);
    expect(buildGaps(undefined)).toEqual([]);
  });

  it('maps missingSkills only to "Missing skill: X" entries', () => {
    expect(buildGaps({ missingSkills: ['Docker', 'Kubernetes'] })).toEqual([
      'Missing skill: Docker',
      'Missing skill: Kubernetes'
    ]);
    expect(MISSING_SKILL_PREFIX).toBe('Missing skill: ');
  });

  it('passes weaknesses only through unchanged', () => {
    expect(buildGaps({ weaknesses: ['No cloud experience', 'Short tenure'] })).toEqual([
      'No cloud experience',
      'Short tenure'
    ]);
  });

  it('skips a missing skill that already appears as a bare weakness (case-insensitive)', () => {
    expect(buildGaps({ weaknesses: ['docker'], missingSkills: ['Docker', 'Kafka'] })).toEqual([
      'docker',
      'Missing skill: Kafka'
    ]);
  });

  it("skips a missing skill that already appears via its 'Missing skill:' label", () => {
    expect(buildGaps({ weaknesses: ['missing skill: docker'], missingSkills: ['Docker'] })).toEqual([
      'missing skill: docker'
    ]);
    expect(buildGaps({ gaps: ['Missing skill:   DOCKER'], missingSkills: [' docker '] })).toEqual([
      'Missing skill:   DOCKER'
    ]);
  });

  it('collapses whitespace and case when deduping, first occurrence wins', () => {
    expect(buildGaps({
      weaknesses: ['  Weak   SQL skills ', 'weak sql\tskills'],
      gaps: ['WEAK SQL SKILLS']
    })).toEqual(['Weak   SQL skills']);
  });

  it('dedupes repeated missing skills', () => {
    expect(buildGaps({ missingSkills: ['Go', ' go ', 'GO'] })).toEqual(['Missing skill: Go']);
  });

  it('combines gaps and missingSkills', () => {
    expect(buildGaps({ gaps: ['Needs leadership experience'], missingSkills: ['AWS'] })).toEqual([
      'Needs leadership experience',
      'Missing skill: AWS'
    ]);
  });

  it('drops junk entries (blank strings, non-strings) and tolerates non-array fields', () => {
    const junk = {
      weaknesses: ['', '   ', null, 42, { a: 1 }, 'Real weakness'],
      gaps: 'not-an-array',
      missingSkills: [undefined, '', '  ', 'Rust']
    } as unknown as JobMatchResult;
    expect(buildGaps(junk)).toEqual(['Real weakness', 'Missing skill: Rust']);
  });

  it('orders weaknesses, then gaps, then missingSkills', () => {
    expect(buildGaps({
      missingSkills: ['Terraform'],
      gaps: ['Gap A'],
      weaknesses: ['Weakness A', 'Weakness B']
    })).toEqual(['Weakness A', 'Weakness B', 'Gap A', 'Missing skill: Terraform']);
  });
});

describe('RiskService', () => {
  let service: RiskService;
  let http: HttpTestingController;

  /** transformToRiskAssessment is private; exercised directly for the scale table. */
  const transform = (result: JobMatchResult): RiskAssessment =>
    (service as unknown as { transformToRiskAssessment(r: JobMatchResult): RiskAssessment })
      .transformToRiskAssessment(result);
  const percent = (fitScore: JobMatchResult['fitScore'], matchScore?: JobMatchResult['matchScore']) =>
    transform({ fitScore, matchScore }).matchPercent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(RiskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('match percent scale (0-1 vs 0-100)', () => {
    it('multiplies 0-1 fractions by 100', () => {
      expect(percent(0.85)).toBe(85);
      expect(percent(0.42)).toBe(42);
    });

    it('treats exactly 1 as a fraction, so 100% (not 1%)', () => {
      expect(percent(1)).toBe(100);
    });

    it('rounds 0-100 values so number and band agree (69.5 -> 70)', () => {
      expect(percent(69.5)).toBe(70);
      expect(percent(80)).toBe(80);
    });

    it('keeps 0 (and "0") as a genuine 0% score', () => {
      expect(percent(0)).toBe(0);
      expect(percent('0')).toBe(0);
    });

    it('accepts numeric strings', () => {
      expect(percent('80')).toBe(80);
      expect(percent(' 0.85 ')).toBe(85);
    });

    it('prefers fitScore over matchScore when both are usable', () => {
      expect(percent(0.9, 0.1)).toBe(90);
      expect(percent(0, 75)).toBe(0);
    });

    it("falls back to matchScore when fitScore is unusable ('' -> '0.42' gives 42)", () => {
      expect(percent('', '0.42')).toBe(42);
      expect(percent(null, 55)).toBe(55);
      expect(percent('abc', '  ')).toBeNull();
    });

    it('clamps out-of-range scores to 0-100', () => {
      expect(percent(-5)).toBe(0);
      expect(percent(150)).toBe(100);
      expect(percent(100.4)).toBe(100);
      expect(percent('-0.2')).toBe(0);
    });

    it('returns null when neither score is usable', () => {
      expect(percent(undefined, undefined)).toBeNull();
      expect(percent(null, null)).toBeNull();
      expect(percent(NaN, Infinity)).toBeNull();
      expect(percent('   ', '')).toBeNull();
    });
  });

  it('analyzeJob posts to /api/job-analysis and maps a null-score result end to end', () => {
    let assessment: RiskAssessment | undefined;
    service.analyzeJob({ email: 'a@b.c', cvText: 'cv', jobDescription: 'jd' })
      .subscribe(a => (assessment = a));

    const req = http.expectOne(`${environment.apiBaseUrl}/api/job-analysis`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c', cvText: 'cv', jobDescription: 'jd' });
    req.flush({
      fitScore: null,
      matchScore: null,
      strengths: ['Angular', 'TypeScript'],
      weaknesses: ['Testing'],
      missingSkills: ['Docker', 'testing'],
      gaps: null,
      roadmap: ['Learn Docker'],
      summary: 'Decent fit'
    });

    expect(assessment).toBeDefined();
    const a = assessment!;
    expect(a.matchPercent).toBeNull();
    expect(a.strengths).toEqual(['Angular', 'TypeScript']);
    expect(a.weaknesses).toEqual(['Testing', 'Missing skill: Docker']);
    // signals: strengths 2 + weaknesses 1 + missingSkills 2 + gaps 0 + roadmap 1 + summary 1
    expect(a.signalsCount).toBe(7);
    expect(a.confidence).toBeCloseTo(0.85, 10);
    expect(a.coverage).toBeCloseTo((5 / 6) * 100, 10);
    expect(a.roadmapPrecision).toBe('HIGH');
    expect(a.roadmapAllowedByDepth).toBeTrue();
    expect(a.summary).toBe('Decent fit');
    expect(a.roadmap).toEqual(['Learn Docker']);
  });

  it('maps an empty backend response to a safe, empty assessment', () => {
    const a = transform({});
    expect(a.matchPercent).toBeNull();
    expect(a.strengths).toEqual([]);
    expect(a.weaknesses).toEqual([]);
    expect(a.signalsCount).toBe(0);
    expect(a.confidence).toBe(0.5);
    expect(a.coverage).toBe(0);
    expect(a.roadmapPrecision).toBe('LOW');
    expect(a.roadmapAllowedByDepth).toBeFalse();
  });
});
