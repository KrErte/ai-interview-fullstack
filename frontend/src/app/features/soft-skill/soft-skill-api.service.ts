import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type SoftSkillSourceType =
  | 'HR'
  | 'TECH'
  | 'TEAM_LEAD'
  | 'AI_INTERVIEW'
  | 'CV_ANALYSIS'
  | 'SELF_ASSESSMENT'
  | 'OTHER';

export interface SoftSkillDimension {
  key: string;
  displayName: string;
  description?: string | null;
}

export interface SoftSkillScoreRequest {
  dimensionKey: string;
  score: number;
  explanation?: string | null;
}

export interface SoftSkillEvaluationRequest {
  email: string;
  jobContext?: string | null;
  sourceType: SoftSkillSourceType;
  sourceLabel?: string | null;
  overallComment?: string | null;
  scores: SoftSkillScoreRequest[];
}

export interface SoftSkillEvaluationResponse {
  id?: string;
  message?: string;
  createdAt?: string;
}

export interface SoftSkillSourceBreakdown {
  sourceType: SoftSkillSourceType | string;
  sourceLabel?: string | null;
  averageScore: number;
  evaluationCount: number;
}

export type SoftSkillConfidence = 'low' | 'medium' | 'high';

export interface SoftSkillMergedDimensionView {
  dimensionKey: string;
  mergedScore: number | null;
  confidence: SoftSkillConfidence;
  sourcesMerged: number;
}

export interface SoftSkillMergedProfile {
  candidateEmail: string;
  summary?: string | null;
  dimensions: SoftSkillMergedDimensionView[];
}

@Injectable({
  providedIn: 'root'
})
export class SoftSkillApiService {
  private readonly baseUrl = '/api/soft-skill';

  constructor(private readonly http: HttpClient) {}

  getDimensions(): Observable<SoftSkillDimension[]> {
    return this.http.get<SoftSkillDimension[]>(`${this.baseUrl}/dimensions`);
  }

  createEvaluation(
    request: SoftSkillEvaluationRequest
  ): Observable<SoftSkillEvaluationResponse> {
    return this.http.post<SoftSkillEvaluationResponse>(
      `${this.baseUrl}/evaluations`,
      request
    );
  }

  getMergedProfile(email: string): Observable<SoftSkillMergedProfile> {
    const params = new HttpParams().set('email', email);

    return this.http.get<SoftSkillMergedProfile>(
      `${this.baseUrl}/merged-profile`,
      { params }
    );
  }
}


