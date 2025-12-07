import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TrainingProgressResponse {
  email: string;
  totalTasks: number;
  completedTasks: number;
  progressPercent: number;
  lastUpdated: string | null;
  completedTaskKeys: string[];
}

export interface TrainingTaskUpdate {
  email: string;
  taskKey: string;
  completed: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class TrainingProgressService {
  private readonly baseUrl = 'http://localhost:8080/api/training';

  constructor(private http: HttpClient) {}

  getProgress(email: string): Observable<TrainingProgressResponse> {
    return this.http.get<TrainingProgressResponse>(`${this.baseUrl}/progress`, {
      params: { email },
    });
  }

  updateTask(payload: TrainingTaskUpdate): Observable<TrainingProgressResponse> {
    return this.http.post<TrainingProgressResponse>(`${this.baseUrl}/task`, payload);
  }

  /**
   * Küsi backendilt järgmine treeneri küsimus.
   */
  getNextQuestion(): Observable<string> {
    // Angular HttpClient nõuab 'text' kui 'json' tüüpi cast'i
    return this.http.get(`${this.baseUrl}/next-question`, {
      responseType: 'text',
    }) as unknown as Observable<string>;
  }
}
