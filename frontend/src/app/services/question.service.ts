import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EvaluateAnswerRequest {
  email: string;
  question: string;
  answer: string;
}

@Injectable({
  providedIn: 'root',
})
export class QuestionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/questions`;

  constructor(private readonly http: HttpClient) {}

  evaluateAnswer(payload: EvaluateAnswerRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/evaluate`, payload);
  }
}
