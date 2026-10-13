import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AssessmentRequest,
  AssessmentResult,
  CategorySummary,
  ProfessionSummary,
  Question
} from '../models/ai-readiness.model';

@Injectable({
  providedIn: 'root'
})
export class AiReadinessService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/ai-readiness`;
  private readonly RESULT_KEY = 'ai_readiness_last_result';

  constructor(private http: HttpClient) {}

  professions(): Observable<ProfessionSummary[]> {
    return this.http.get<ProfessionSummary[]>(`${this.baseUrl}/professions`);
  }

  /** Searches all ~900 professions by name and alternate title; an empty query with a category lists that group. */
  searchProfessions(query: string, category: string | null, limit = 50): Observable<ProfessionSummary[]> {
    let params = new HttpParams().set('q', query).set('limit', limit);
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<ProfessionSummary[]>(`${this.baseUrl}/professions/search`, { params });
  }

  categories(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>(`${this.baseUrl}/professions/categories`);
  }

  questions(): Observable<Question[]> {
    return this.http.get<Question[]>(`${this.baseUrl}/questions`);
  }

  assess(request: AssessmentRequest): Observable<AssessmentResult> {
    return this.http.post<AssessmentResult>(`${this.baseUrl}/assess`, request);
  }

  saveLastResult(result: AssessmentResult): void {
    try {
      localStorage.setItem(this.RESULT_KEY, JSON.stringify(result));
    } catch {
      // storage unavailable – result just won't be remembered
    }
  }

  loadLastResult(): AssessmentResult | null {
    try {
      const raw = localStorage.getItem(this.RESULT_KEY);
      return raw ? (JSON.parse(raw) as AssessmentResult) : null;
    } catch {
      return null;
    }
  }

  clearLastResult(): void {
    try {
      localStorage.removeItem(this.RESULT_KEY);
    } catch {
      // ignore
    }
  }
}
