import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InterviewAnswerRequest,
  InterviewNextQuestionResponse,
  InterviewQuestionResponse,
  InterviewSessionStartRequest,
  InterviewSummaryResponse
} from '../models/interview-session.model';
import { environment } from '../../../environments/environment';

type AnswerOrSummary = InterviewNextQuestionResponse | InterviewSummaryResponse;

@Injectable({ providedIn: 'root' })
export class InterviewSessionApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/interview/session`;

  constructor(private http: HttpClient) {}

  startSession(request: InterviewSessionStartRequest): Observable<InterviewQuestionResponse> {
    return this.http.post<InterviewQuestionResponse>(`${this.baseUrl}/start`, request);
  }

  answer(request: InterviewAnswerRequest): Observable<AnswerOrSummary> {
    return this.http.post<AnswerOrSummary>(`${this.baseUrl}/answer`, request);
  }
}


