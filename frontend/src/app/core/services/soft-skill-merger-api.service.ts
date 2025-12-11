import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

export type SoftSkillSourceType =
  | 'HR'
  | 'TECH'
  | 'TEAM_LEAD'
  | 'AI_INTERVIEW'
  | 'CV_ANALYSIS'
  | 'SELF_ASSESSMENT'
  | 'OTHER';

export interface SoftSkillDimensionScoreInput {
  dimension: string;
  score: number | null;
  explanation?: string | null;
}

export interface SoftSkillSourceDto {
  sourceType: SoftSkillSourceType | string;
  label?: string | null;
  content: string;
  dimensionScores?: SoftSkillDimensionScoreInput[];
}

export interface SoftSkillMergeRequest {
  email: string;
  jobContext?: string | null;
  sources: SoftSkillSourceDto[];
  saveMerged?: boolean;
}

export interface MergedDimensionScore {
  dimension: string;
  mergedScore: number | null;
  confidence: number | null;
  rationale: string | null;
}

export interface SoftSkillMergeMeta {
  overallConfidence: number | null;
  mainDisagreements: string[];
  notesForCoach: string[];
}

export interface SoftSkillMergedProfile {
  summary: string;
  strengths: string[];
  risks: string[];
  communicationStyle: string;
  collaborationStyle: string;
  growthAreas: string[];
  dimensionScoresMerged: MergedDimensionScore[];
  meta?: SoftSkillMergeMeta | null;
}

export interface SoftSkillMergeResponse {
  mergedProfile: SoftSkillMergedProfile;
  saved: boolean;
  savedProfileId: string | null;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class SoftSkillMergerApiService {
  private readonly baseUrl = '/api/soft-skills';
  private readonly legacyBaseUrl = '/api/soft-skill';

  constructor(private readonly http: HttpClient) {}

  mergeSoftSkills(
    request: SoftSkillMergeRequest
  ): Observable<SoftSkillMergeResponse> {
    return this.http
      .post<SoftSkillMergeResponse>(`${this.baseUrl}/merge`, request)
      .pipe(
        catchError(err => {
          // Gracefully fall back to singular endpoint if backend routing differs.
          if (err?.status === 404 || err?.status === 405) {
            return this.http.post<SoftSkillMergeResponse>(
              `${this.legacyBaseUrl}/merge`,
              request
            );
          }
          return throwError(() => err);
        })
      );
  }
}
