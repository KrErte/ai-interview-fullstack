import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { RiskAssessment, JobMatchResult, CvUploadResponse } from '../models/risk.model';

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
   * Analyze job match and get risk assessment
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
   * Transform backend response to RiskAssessment model
   */
  private transformToRiskAssessment(result: JobMatchResult): RiskAssessment {
    const rawScore = result.fitScore ?? result.matchScore ?? 0;
    const riskPercent = rawScore > 1 ? rawScore : Math.round(rawScore * 100);

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
      riskPercent,
      confidence,
      signalsCount,
      coverage,
      strengths: result.strengths || [],
      weaknesses: [...(result.weaknesses || []), ...(result.gaps || [])],
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
