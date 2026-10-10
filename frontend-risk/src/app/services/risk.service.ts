import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { RiskAssessment, JobMatchResult, CvUploadResponse } from '../models/risk.model';

/**
 * Returns a finite number for a number or a numeric string, otherwise null.
 * Strings are trimmed; empty/whitespace-only and non-numeric strings are
 * treated as missing (null). Number('') would be 0, so the empty check matters.
 * Booleans, objects, NaN and +/-Infinity are also null.
 */
export function toFiniteNumber(value: unknown): number | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (trimmed === '') return null;
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

/**
 * Clamps a match percentage to the 0-100 range. Call after rounding so that
 * out-of-range backend values (-5, 150) still produce a valid score/band.
 */
export function clampPercent(value: number): number {
  return Math.min(100, Math.max(0, value));
}

/**
 * Rounds a percentage to an integer ONCE, after removing binary float noise.
 *
 * 0.285 * 100 is 28.499999999999996 in IEEE-754, so a bare Math.round() gives
 * 28 instead of 29 (same for 0.145, 0.565, 0.575). toPrecision(12) snaps the
 * product back to 28.5 first. Do not "fix" this by double rounding
 * (Math.round(raw * 1000) / 10 and then Math.round again): that turns 0.6949
 * (69.49) into 69.5 -> 70 and moves it into the wrong band.
 */
export function roundPercent(value: number): number {
  return Math.round(Number(value.toPrecision(12)));
}

/**
 * Converts a raw backend score into the integer 0-100 match percent, or null.
 *
 * Score-scale contract (confirmed by Backend): matchScore/fitScore in the
 * /api/job-analysis response is NOT normalised.
 *   - Fallback analyser: always a 0-1 fraction (in practice 0.3-0.9).
 *   - OpenAI analyser: raw model output, either 0-1 or 0-100, and it can be
 *     out of range (e.g. 1.5 or > 100).
 * Frontend rule, mirroring the backend's normalizeScore:
 *   pct = score <= 1 ? score * 100 : score, then round once, then clamp 0-100.
 *   - exactly 1 is a fraction -> 100 (not 1%).
 *   - values in (1, 2) are treated as a percent: 1.2 -> 1, 1.5 -> 2.
 *   - 150 -> 100, -5 -> 0 (negatives are <= 1 so are x100, then clamped).
 *   - 0 / "0" is a genuine 0% score, not "missing".
 *   - numeric strings are accepted ("80" -> 80) via toFiniteNumber().
 *   - null/undefined/NaN/Infinity, blank or non-numeric strings -> null
 *     (no score; the snapshot card shows its empty state).
 * Backend may later normalise to 0-100 on its side; this rule stays correct
 * for that (any value > 1 passes through), except a true 1% would read as
 * 100%, so revisit the "<= 1" branch if/when that change lands.
 */
export function toMatchPercent(score: unknown): number | null {
  const raw = toFiniteNumber(score);
  if (raw === null) return null;
  const pct = raw <= 1 ? raw * 100 : raw;
  return clampPercent(roundPercent(pct));
}

/** Prefix used for gap entries derived from the backend's missingSkills. */
export const MISSING_SKILL_PREFIX = 'Missing skill: ';

/** Case-insensitive, whitespace-normalised key used to dedupe gap entries. */
function gapKey(text: string): string {
  return text.trim().replace(/\s+/g, ' ').toLowerCase();
}

/**
 * Keeps only non-blank strings, trimmed. Tolerates null/undefined/non-array input.
 * Shared by buildGaps() and the signals/coverage counts so they agree.
 */
export function cleanStrings(list: unknown): string[] {
  if (!Array.isArray(list)) return [];
  return list
    .filter((v): v is string => typeof v === 'string')
    .map(v => v.trim())
    .filter(v => v !== '');
}

/**
 * Builds the gaps list shown in the "Areas to Improve" card from the backend's
 * weaknesses, gaps and missingSkills (all string[] on the wire; any of them may
 * be missing or null).
 *
 * - weaknesses first, then gaps, then missingSkills as "Missing skill: X".
 * - Deduped case-insensitively (trimmed, whitespace collapsed), first
 *   occurrence wins. A missing skill is skipped when an earlier entry equals
 *   either the bare skill ("Docker") or its label ("Missing skill: Docker").
 * - Empty only when all three sources are empty, so "No major gaps found" is
 *   never shown while the backend reports missing skills.
 */
export function buildGaps(result: Pick<JobMatchResult, 'weaknesses' | 'gaps' | 'missingSkills'> | null | undefined): string[] {
  const out: string[] = [];
  const seen = new Set<string>();
  const add = (label: string, ...keys: string[]) => {
    if (keys.some(k => seen.has(gapKey(k)))) return;
    keys.forEach(k => seen.add(gapKey(k)));
    out.push(label);
  };
  for (const w of [...cleanStrings(result?.weaknesses), ...cleanStrings(result?.gaps)]) {
    add(w, w);
  }
  for (const skill of cleanStrings(result?.missingSkills)) {
    const label = MISSING_SKILL_PREFIX + skill;
    add(label, label, skill);
  }
  return out;
}

export interface JobMatchRequest {
  email?: string;
  cvText: string;
  jobDescription?: string;
  targetRole?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RiskService {
  private readonly baseUrl = environment.apiBaseUrl;
  private readonly CV_KEY = 'risk_cv_text';
  private cachedCvText: string | null = localStorage.getItem(this.CV_KEY);

  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  /**
   * Upload and parse CV file
   */
  uploadCv(file: File): Observable<CvUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CvUploadResponse>(`${this.baseUrl}/api/cv/extract-text`, formData);
  }

  /**
   * Analyze how well the CV matches the job description
   */
  analyzeJob(payload: JobMatchRequest): Observable<RiskAssessment> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<JobMatchResult>(`${this.baseUrl}/api/job-analysis`, {
      email,
      cvText: payload.cvText || '',
      jobDescription: payload.jobDescription || ''
    }).pipe(
      map(result => this.transformToRiskAssessment(result))
    );
  }

  /**
   * Generate roadmap based on current assessment
   */
  generateRoadmap(payload: JobMatchRequest): Observable<string[]> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<JobMatchResult>(`${this.baseUrl}/api/job-analysis`, {
      email,
      cvText: payload.cvText || '',
      jobDescription: payload.jobDescription || ''
    }).pipe(
      map(result => result.roadmap || [])
    );
  }

  /**
   * Transform backend job-analysis response to the job match model (RiskAssessment)
   */
  private transformToRiskAssessment(result: JobMatchResult): RiskAssessment {
    // CV-to-job match score (higher = better match), integer 0-100 or null.
    // This is NOT a replaceability / risk score. fitScore wins when both
    // fitScore and matchScore are usable. See toMatchPercent() for the full
    // score-scale contract: the backend score is not normalised (fallback is a
    // 0-1 fraction, OpenAI may send 0-1 or 0-100, even 1.5 or > 100), so
    // pct = score <= 1 ? score * 100 : score, rounded once (float noise
    // removed), clamped to 0-100; null/invalid means no score.
    const matchPercent = [result.fitScore, result.matchScore]
      .map(toMatchPercent)
      .find((v): v is number => v !== null) ?? null;

    // Gap entries exactly as shown in "Areas to Improve" (cleaned + deduped).
    const gaps = buildGaps(result);
    const strengths = cleanStrings(result.strengths);
    const roadmapSteps = cleanStrings(result.roadmap);
    const hasSummary = typeof result.summary === 'string' && result.summary.trim() !== '';

    // "Based on N signals": counts the cleaned lists, so blanks/junk and
    // duplicate gaps that buildGaps() drops are not counted. Weaknesses, gaps
    // and missingSkills contribute buildGaps().length (not their raw lengths).
    const signalsCount = strengths.length + gaps.length + roadmapSteps.length + (hasSummary ? 1 : 0);

    // Confidence based on signals (more signals = higher confidence)
    const confidence = Math.min(0.95, 0.5 + (signalsCount * 0.05));

    // Coverage: share of the 6 backend categories that hold at least one
    // real (non-blank string) entry. A list of only blanks/junk is absent.
    const categoriesPresent = [
      strengths.length > 0,
      cleanStrings(result.weaknesses).length > 0,
      cleanStrings(result.missingSkills).length > 0,
      cleanStrings(result.gaps).length > 0,
      roadmapSteps.length > 0,
      hasSummary
    ];
    const possibleSignals = categoriesPresent.length;
    const coverage = (categoriesPresent.filter(Boolean).length / possibleSignals) * 100;

    // Determine roadmap precision based on coverage
    let roadmapPrecision: 'LOW' | 'MEDIUM' | 'HIGH' = 'LOW';
    if (coverage >= 80) roadmapPrecision = 'HIGH';
    else if (coverage >= 50) roadmapPrecision = 'MEDIUM';

    return {
      matchPercent,
      confidence,
      signalsCount,
      coverage,
      strengths: result.strengths || [],
      weaknesses: gaps,
      roadmapAllowedByDepth: coverage >= 30,
      roadmapPrecision,
      summary: result.summary,
      roadmap: result.roadmap
    };
  }

  setCachedCv(text: string): void {
    this.cachedCvText = text;
    localStorage.setItem(this.CV_KEY, text || '');
  }

  getCachedCv(): string {
    return this.cachedCvText || '';
  }
}
