import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { JOB_MATCH_SEED_RESULTS } from '../mock-data/job-match.seed';
import { JOB_ANALYSIS_SEED_LATEST } from '../mock-data/job-analysis.seed';
import { CV_UPLOAD_SEED_RESPONSE } from '../mock-data/cv.seed';

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
    return this.http
      .post<CvUploadResponse>(`${this.baseUrl}/api/cv/extract-text`, formData)
      .pipe(
        map((response) => response || CV_UPLOAD_SEED_RESPONSE),
        catchError(() => of(CV_UPLOAD_SEED_RESPONSE))
      );
  }

  matchJobs(payload: JobMatchRequest): Observable<JobMatchResult[]> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http
      .post<JobMatchResult[]>(`${this.baseUrl}/api/job-match`, {
        ...payload,
        email,
      })
      .pipe(
        map((response) =>
          response && response.length ? response : JOB_MATCH_SEED_RESULTS
        ),
        catchError(() => of(JOB_MATCH_SEED_RESULTS))
      );
  }

  analyzeJob(payload: JobMatchRequest): Observable<JobMatchResult> {
    const email = payload.email || this.auth.getCurrentUserEmail() || '';
    return this.http
      .post<JobMatchResult>(`${this.baseUrl}/api/job-analysis`, {
        email,
        cvText: payload.cvText || '',
        jobDescription: payload.jobDescription || '',
      })
      .pipe(
        map((response) => response || JOB_ANALYSIS_SEED_LATEST),
        catchError(() => of(JOB_ANALYSIS_SEED_LATEST))
      );
  }

  setCachedCv(text: string) {
    this.cachedCvText = text;
    localStorage.setItem(this.CV_KEY, text || '');
  }

  getCachedCv(): string {
    return this.cachedCvText || '';
  }
}

