import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface CvUploadResponse {
  text: string;
  headline?: string;
  skills?: string[];
  experienceSummary?: string;
}

export interface JobMatchRequest {
  email?: string;
  cvText: string;
  jobDescription?: string;
  targetRole?: string;
}

export interface JobMatchResult {
  jobTitle?: string;
  jobDescription?: string;
  fitScore?: number;
  matchScore?: number;
  strengths?: string[];
  weaknesses?: string[];
  missingSkills?: string[];
  gaps?: string[];
  roadmap?: string[];
  suggestedImprovements?: string;
  summary?: string;
}

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly baseUrl = environment.apiBaseUrl;
  private readonly CV_KEY = 'aiim_cv_text';
  private cachedCvText: string | null = localStorage.getItem(this.CV_KEY);

  constructor(private http: HttpClient, private auth: AuthService) {}

  uploadCv(file: File): Observable<CvUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CvUploadResponse>(`${this.baseUrl}/api/cv/extract-text`, formData);
  }

  matchJobs(payload: JobMatchRequest): Observable<JobMatchResult[]> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<JobMatchResult[]>(`${this.baseUrl}/api/job-match`, {
      ...payload,
      email
    });
  }

  analyzeJob(payload: JobMatchRequest): Observable<JobMatchResult> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<JobMatchResult>(`${this.baseUrl}/api/job-analysis`, {
      email,
      cvText: payload.cvText || '',
      jobDescription: payload.jobDescription || ''
    });
  }

  setCachedCv(text: string) {
    this.cachedCvText = text;
    localStorage.setItem(this.CV_KEY, text || '');
  }

  getCachedCv(): string {
    return this.cachedCvText || '';
  }
}

