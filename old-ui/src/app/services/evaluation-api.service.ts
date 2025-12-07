import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EvaluateAnswerRequest {
  email?: string;
  questionId?: string;
  question?: string;
  answer: string;
}

export interface EvaluateAnswerResponse {
  score: number;
  strengths: string;
  weaknesses: string;
  suggestions: string;
  fallback: boolean;
}

@Injectable({ providedIn: 'root' })
export class EvaluationApiService {
  private readonly baseUrl = 'http://localhost:8080/api/questions';

  constructor(private http: HttpClient) {}

  evaluate(req: EvaluateAnswerRequest): Observable<EvaluateAnswerResponse> {
    return this.http.post<EvaluateAnswerResponse>(
      `${this.baseUrl}/evaluate`,
      req
    );
  }
}
