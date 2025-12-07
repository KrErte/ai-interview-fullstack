import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SkillPlanRequest,
  SkillPlanResponse
} from '../models/skill-plan.model';

@Injectable({
  providedIn: 'root'
})
export class SkillPlanService {

  // DEV: lihtne ja üheselt selge, et läheb õigesse kohta
  private readonly baseUrl = 'http://localhost:8080/api/skill-plan';

  constructor(private http: HttpClient) {}

  generatePlan(payload: SkillPlanRequest): Observable<SkillPlanResponse> {
    return this.http.post<SkillPlanResponse>(this.baseUrl, payload);
  }
}
