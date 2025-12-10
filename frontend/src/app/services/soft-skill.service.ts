import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type SoftSkillDimension =
  | 'ADAPTABILITY'
  | 'GROWTH_MINDSET'
  | 'COMMUNICATION'
  | 'COLLABORATION'
  | 'OWNERSHIP'
  | 'PROBLEM_SOLVING'
  | 'LEADERSHIP';

export type SoftSkillSource = 'HR' | 'TECH_LEAD' | 'TEAM_LEAD';

export interface SoftSkillEvaluationRequest {
  email: string;
  dimension: SoftSkillDimension;
  source: SoftSkillSource;
  score: number;
  comment: string;
}

export interface SoftSkillEvaluationResponse extends SoftSkillEvaluationRequest {
  id?: string;
  createdAt?: string;
}

export interface SoftSkillMergedDimension {
  dimension: string;
  mergedScore: number;
  explanation: string;
}

export interface SoftSkillMergedProfileResponse {
  email: string;
  overallScore: number;
  createdAt: string;
  updatedAt: string;
  dimensions: SoftSkillMergedDimension[];
}

@Injectable({
  providedIn: 'root'
})
export class SoftSkillService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/soft-skills`;

  constructor(private readonly http: HttpClient) {}

  getEvaluations(email: string): Observable<SoftSkillEvaluationResponse[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<SoftSkillEvaluationResponse[]>(`${this.baseUrl}/evaluations`, { params });
  }

  createEvaluation(
    request: SoftSkillEvaluationRequest
  ): Observable<SoftSkillEvaluationResponse> {
    return this.http.post<SoftSkillEvaluationResponse>(
      `${this.baseUrl}/evaluations`,
      request
    );
  }

  mergeProfile(email: string): Observable<SoftSkillMergedProfileResponse> {
    const params = new HttpParams().set('email', email);
    return this.http.post<SoftSkillMergedProfileResponse>(
      `${this.baseUrl}/merge`,
      {},
      { params }
    );
  }

  getMergedProfile(email: string): Observable<SoftSkillMergedProfileResponse> {
    const params = new HttpParams().set('email', email);
    return this.http.get<SoftSkillMergedProfileResponse>(
      `${this.baseUrl}/profile`,
      { params }
    );
  }
}


