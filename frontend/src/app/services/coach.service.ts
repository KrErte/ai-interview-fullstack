import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MindsetRoadmapSummary {
  roadmapKey: string;
  title: string;
  totalTasks: number;
  completedTasks: number;
  progressPercent: number;
}

export interface MindsetTask {
  taskKey: string;
  completed: boolean;
  score?: number | null;
  updatedAt?: string | null;
}

export interface MindsetRoadmapDetail {
  summary: MindsetRoadmapSummary;
  tasks: MindsetTask[];
}

export interface HistoryTurn {
  question: string;
  answer: string;
}

export interface SoftSkillQuestionRequest {
  email: string;
  roadmapKey: string;
  previousQuestion?: string | null;
  previousAnswer?: string | null;
  history?: HistoryTurn[];
}

export interface SoftSkillQuestionResponse {
  questionText: string;
  difficulty: string;
  fallback: boolean;
  score?: number | null;
  coachFeedback?: string | null;
  weakestSkill?: string | null;
}

export interface AdaptiveAnalysisRequest {
  email: string;
  roadmapItemId?: string | null;
  question: string;
  answer: string;
  currentSkillSnapshot?: Record<string, number>;
}

export interface AdaptiveAnalysisResponse {
  feedback?: string;
  nextQuestion?: string;
  weakestSkill?: string;
  updatedSkills?: Record<string, number>;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CoachService {
  private readonly mindsetBase = `${environment.apiBaseUrl}/api/mindset`;
  private readonly trainerBase = `${environment.apiBaseUrl}/api/trainer`;
  private readonly skillCoachBase = `${environment.apiBaseUrl}/api/skill-coach`;

  constructor(private readonly http: HttpClient) {}

  getMindsetRoadmaps(email: string): Observable<MindsetRoadmapSummary[]> {
    return this.http.get<MindsetRoadmapSummary[]>(`${this.mindsetBase}/roadmaps`, {
      params: { email }
    });
  }

  getMindsetRoadmapDetail(email: string, roadmapKey: string): Observable<MindsetRoadmapDetail> {
    return this.http.get<MindsetRoadmapDetail>(`${this.mindsetBase}/roadmaps/${encodeURIComponent(roadmapKey)}`, {
      params: { email }
    });
  }

  askSoftQuestion(payload: SoftSkillQuestionRequest): Observable<SoftSkillQuestionResponse> {
    return this.http.post<SoftSkillQuestionResponse>(`${this.trainerBase}/soft-question`, payload);
  }

  analyzeAndStore(payload: AdaptiveAnalysisRequest): Observable<AdaptiveAnalysisResponse> {
    return this.http.post<AdaptiveAnalysisResponse>(`${this.skillCoachBase}/answer`, payload);
  }
}

