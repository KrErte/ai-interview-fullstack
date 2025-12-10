import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { TRAINING_PROGRESS_SEED } from '../mock-data/training.seed';

export interface TrainingProgress {
  email: string;
  totalTasks: number;
  completedTasks: number;
  totalJobAnalyses: number;
  totalTrainingSessions: number;
  trainingProgressPercent: number;
  status: string;
  lastActivityAt?: string;
  lastMatchScore?: number;
  lastMatchSummary?: string;
}

export interface TrainingTaskRequest {
  email?: string;
  taskKey: string;
  skillKey?: string;
  question?: string;
  answer?: string;
  answerText?: string;
  completed?: boolean;
  score?: number;
}

@Injectable({ providedIn: 'root' })
export class TrainingService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  getProgress(email?: string): Observable<TrainingProgress> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', targetEmail);
    return this.http
      .get<TrainingProgress>(`${this.baseUrl}/api/training/progress`, { params })
      .pipe(
        map((response) => response || TRAINING_PROGRESS_SEED),
        catchError(() => of(TRAINING_PROGRESS_SEED))
      );
  }

  updateTask(request: TrainingTaskRequest): Observable<TrainingProgress> {
    const email = request.email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<TrainingProgress>(`${this.baseUrl}/api/training/task`, {
      ...request,
      email
    });
  }

  completeTask(taskKey: string, skillKey?: string): Observable<TrainingProgress> {
    return this.updateTask({
      taskKey,
      skillKey,
      completed: true
    });
  }

  startPlan(tasks: any[], targetRole?: string): Observable<any> {
    const email = this.auth.getCurrentUserEmail() || '';
    return this.http.post(`${this.baseUrl}/api/training/start`, {
      email,
      targetRole,
      tasks
    });
  }

  completeRoadmapTask(taskKey: string): Observable<TrainingProgress> {
    const email = this.auth.getCurrentUserEmail() || '';
    return this.http.post<TrainingProgress>(`${this.baseUrl}/api/training/task/complete`, {
      email,
      taskKey,
      completed: true
    });
  }
}

