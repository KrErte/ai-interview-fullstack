import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CvUploadResponse {
  text: string;
}

export interface JobMatchRequest {
  cvText: string;
  jobDescription: string;
  email?: string;
}

export interface JobMatchResult {
  matchScore: number | null;
  strengths?: string[];
  weaknesses?: string[];
  missingSkills?: string[];
  roadmap?: string[];
  suggestedImprovements?: string;
  summary?: string;
  trainingSessionId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class JobMatchService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  constructor(private readonly http: HttpClient) {}

  uploadCv(file: File): Observable<CvUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CvUploadResponse>(
      `${this.baseUrl}/cv/extract-text`,
      formData
    );
  }

  analyzeMatch(payload: JobMatchRequest): Observable<JobMatchResult> {
    return this.http.post<JobMatchResult>(
      `${this.baseUrl}/job-analysis`,
      payload
    );
  }
}

