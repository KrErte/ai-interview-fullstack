import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type TrainingStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';

export interface TrainingProgressResponse {
  email: string;
  totalTasks: number;
  completedTasks: number;
  totalJobAnalyses: number;
  totalTrainingSessions: number;
  trainingProgressPercent: number;
  status: TrainingStatus;
  lastActivityAt: string | null;
  lastMatchScore: number | null;
  lastMatchSummary: string | null;
}

export interface TrainingTaskRequest {
  email: string;
  taskKey: string;
  question?: string;
  answer?: string;
  answerText?: string;
  completed?: boolean;
  score?: number;
}

export interface AdaptiveAnalysisRequest {
  email: string;
  roadmapItemId?: string | null;
  question: string;
  answer: string;
}

export interface AdaptiveAnalysisResponse {
  feedback: string;
  nextQuestion?: string;
  weakestSkill?: string;
  updatedSkills?: Record<string, number>;
  error?: string;
}

@Injectable({
  providedIn: 'root',
})
export class TrainingProgressService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/training`;
  private readonly coachUrl = `${environment.apiBaseUrl}/api/skill-coach`;

  constructor(private http: HttpClient) {}

  getProgress(email: string): Observable<TrainingProgressResponse> {
    return this.http.get<TrainingProgressResponse>(`${this.baseUrl}/progress`, {
      params: { email },
    });
  }

  updateTask(payload: TrainingTaskRequest): Observable<TrainingProgressResponse> {
    return this.http.post<TrainingProgressResponse>(`${this.baseUrl}/task`, payload);
  }

  getNextQuestion(): Observable<string> {
    return this.http.get(`${this.baseUrl}/next-question`, {
      responseType: 'text',
    }) as unknown as Observable<string>;
  }

  answerAndSave(payload: AdaptiveAnalysisRequest): Observable<AdaptiveAnalysisResponse> {
    return this.http.post<AdaptiveAnalysisResponse>(`${this.coachUrl}/answer`, payload);
  }
}
