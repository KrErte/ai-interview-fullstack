import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { JobAnalysisRequest } from '../models/job-analysis-request.model';
import { JobAnalysisResponse } from '../models/job-analysis-response.model';

@Injectable({
  providedIn: 'root',
})
export class JobAnalysisService {
  // eeldus: environment.apiUrl = 'http://localhost:8080/api' vms
  private readonly baseUrl = `${environment.apiBaseUrl}/job-analysis`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Põhi-analüüsi kutsumine (Job Matcher).
   */
  analyze(payload: JobAnalysisRequest): Observable<JobAnalysisResponse> {
    return this.http.post<JobAnalysisResponse>(`${this.baseUrl}`, payload);
  }

  /**
   * Statistika ühe emaili kohta.
   * Backendis võiks olla: GET /api/job-analysis/stats?email=...
   */
  getStatsForEmail(email: string): Observable<any> {
    const params = new HttpParams().set('email', email);
    return this.http.get<any>(`${this.baseUrl}/stats`, { params });
  }

  /**
   * Analüüsi ajalugu ühe emaili kohta.
   * Backendis võiks olla: GET /api/job-analysis/history?email=...
   */
  getHistoryForEmail(email: string): Observable<any[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<any[]>(`${this.baseUrl}/history`, { params });
  }
}
