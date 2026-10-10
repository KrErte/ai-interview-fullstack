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

/** Prefix used for gap entries derived from the backend's missingSkills. */
export const MISSING_SKILL_PREFIX = 'Missing skill: ';

/** Case-insensitive, whitespace-normalised key used to dedupe gap entries. */
function gapKey(text: string): string {
  return text.trim().replace(/\s+/g, ' ').toLowerCase();
}

/** Keeps only non-blank strings, trimmed. Tolerates null/undefined/non-array input. */
function cleanStrings(list: unknown): string[] {
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
    // CV-to-job match score (higher = better match), normalised to 0-100.
    // This is NOT a replaceability / risk score.
    //
    // Scale contract for the backend fitScore / matchScore (fitScore wins when
    // both are usable):
    //   - raw > 1  -> already on a 0-100 scale:  80   -> 80
    //   - raw <= 1 -> a 0-1 fraction, x100:      0.85 -> 85
    //   - raw === 1 exactly is a 0-1 fraction, so it means 100% (not 1%).
    //   - 0 (or "0") is a genuine 0% score, not "missing".
    // Both branches are rounded so the displayed number and the band label
    // agree (e.g. 69.5 -> 70), then clamped to 0-100 (-5 -> 0, 150 -> 100).
    //
    // Numeric strings ("80", " 0.85 ") are accepted via toFiniteNumber().
    // null/undefined/NaN/Infinity, empty or whitespace-only strings and
    // non-numeric strings count as missing: matchPercent stays null and the
    // snapshot card shows its empty state.
    const rawScore = [result.fitScore, result.matchScore]
      .map(toFiniteNumber)
      .find((v): v is number => v !== null);
    const matchPercent = rawScore === undefined
      ? null
      : clampPercent(rawScore > 1 ? Math.round(rawScore) : Math.round(rawScore * 100));

    // Calculate confidence based on available data signals
    const signals = [
      result.strengths?.length || 0,
      result.weaknesses?.length || 0,
      result.missingSkills?.length || 0,
      result.gaps?.length || 0,
      result.roadmap?.length || 0,
      result.summary ? 1 : 0
    ];
    const signalsCount = signals.reduce((a, b) => a + b, 0);

    // Confidence based on signals (more signals = higher confidence)
    const confidence = Math.min(0.95, 0.5 + (signalsCount * 0.05));

    // Coverage based on how many data points we have
    const possibleSignals = 6;
    const coverage = (signals.filter(s => s > 0).length / possibleSignals) * 100;

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
      weaknesses: buildGaps(result),
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
