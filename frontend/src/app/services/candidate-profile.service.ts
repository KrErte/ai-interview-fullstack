import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProgressSummary } from '../models/progress-summary.model';
import { PlanFromCvAndJob } from '../models/plan-cv-job.model';


@Injectable({
  providedIn: 'root'
})
export class CandidateProfileService {

  // vajadusel tõsta environmenti
  private readonly baseUrl = 'http://localhost:8080/api/candidate';

  constructor(private http: HttpClient) {}

  getProgress(email: string): Observable<ProgressSummary> {
    const params = new HttpParams().set('email', email);
    return this.http.get<ProgressSummary>(`${this.baseUrl}/progress`, { params });
  }
  generateLearningPlan(req: { email: string; cvText: string; jobAd: string }) {
  return this.http.post<PlanFromCvAndJob>(
    'http://localhost:8080/api/candidate/learning-plan',
    req
  );
}

}
