import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AggregatedResult,
  SkillMatrix,
} from '../models/evaluation.model';
import {
  InterviewSession,
  StartInterviewRequest,
  StartInterviewResponse,
  SubmitAnswerRequest,
  SubmitAnswerResponse,
} from '../models/interview.model';

@Injectable({
  providedIn: 'root',
})
export class SkillMatrixApiService {
  private readonly baseUrl =
    environment.apiBaseUrl || environment.apiUrl || '';

  constructor(private http: HttpClient) {}

  startInterview(
    payload: StartInterviewRequest
  ): Observable<StartInterviewResponse> {
    return this.http.post<StartInterviewResponse>(
      `${this.baseUrl}/api/skill-matrix/start`,
      payload
    );
  }

  submitAnswer(
    payload: SubmitAnswerRequest
  ): Observable<SubmitAnswerResponse> {
    return this.http.post<SubmitAnswerResponse>(
      `${this.baseUrl}/api/skill-matrix/answer`,
      payload
    );
  }

  getSession(sessionId: string): Observable<InterviewSession> {
    return this.http.get<InterviewSession>(
      `${this.baseUrl}/api/skill-matrix/${sessionId}`
    );
  }

  getFinalResult(sessionId: string): Observable<AggregatedResult> {
    return this.http.get<AggregatedResult>(
      `${this.baseUrl}/api/skill-matrix/${sessionId}/result`
    );
  }
}


