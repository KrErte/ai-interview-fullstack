import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SkillPlanRequest,
  SkillPlanResponse
} from '../models/skill-plan.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SkillPlanService {

  private readonly baseUrl = `${environment.apiBaseUrl}/api/skill-plan`;

  constructor(private http: HttpClient) {}

  generatePlan(payload: SkillPlanRequest): Observable<SkillPlanResponse> {
    return this.http.post<SkillPlanResponse>(this.baseUrl, payload);
  }
}
