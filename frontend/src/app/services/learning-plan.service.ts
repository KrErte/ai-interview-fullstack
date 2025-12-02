import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LearningPlan } from '../models/learning-plan.model';

@Injectable({ providedIn: 'root' })
export class LearningPlanService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getLastPlan(email: string): Observable<LearningPlan> {
    return this.http.get<LearningPlan>(`${this.baseUrl}/plan/last`, {
      params: { email }
    });
  }
}
