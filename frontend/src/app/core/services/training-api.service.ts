import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TrainingTask {
  taskKey: string;
  completed: boolean;
  completedAt: string | null;
}

export interface TrainingStatus {
  totalTasks: number;
  completedTasks: number;
  progressPercent: number;
  tasks: TrainingTask[];
}

export interface TrainingTaskDetail {
  title: string;
  description: string;
}

export const TRAINING_TASK_DETAILS: Record<string, TrainingTaskDetail> = {
  'cv-refresh': {
    title: 'Refresh your CV headline',
    description: 'Tighten your CV headline to clearly describe your target role and strengths.'
  },
  'star-stories': {
    title: 'Draft 3 STAR stories',
    description:
      'Write 3 short STAR stories covering ownership, debugging, and collaborating with difficult stakeholders.'
  },
  'system-design-outline': {
    title: 'Outline a small system design',
    description:
      'Prepare a 15-minute walkthrough of designing a feature flag service or notification system.'
  },
  'frontend-performance': {
    title: 'Review frontend performance basics',
    description:
      'Revise core performance concepts: bundle splitting, code-splitting, lazy loading, and caching.'
  }
};

@Injectable({ providedIn: 'root' })
export class TrainingApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/training`;

  constructor(private http: HttpClient) {}

  getTrainingStatus(): Observable<TrainingStatus> {
    return this.http.get<TrainingStatus>(`${this.baseUrl}/status`);
  }

  setTaskCompleted(taskKey: string, completed: boolean): Observable<TrainingStatus> {
    return this.http.post<TrainingStatus>(`${this.baseUrl}/complete/${encodeURIComponent(taskKey)}`, {
      completed
    });
  }
}

