import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdaptiveQuestionRequest {
  email: string;
  previousQuestion?: string;
  previousAnswer?: string;
  previousScore?: number;
}

export interface AdaptiveQuestionResponse {
  questionText: string;
  fromHistory?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AdaptiveTrainerService {
  private readonly baseUrl = '/api/trainer';

  constructor(private http: HttpClient) {}

  /**
   * Küsi backendilt järgmist adaptiivset küsimust.
   */
  getNextQuestion(
    payload: AdaptiveQuestionRequest
  ): Observable<AdaptiveQuestionResponse> {
    return this.http.post<AdaptiveQuestionResponse>(
      `${this.baseUrl}/adaptive-question`,
      payload
    );
  }

  /**
   * Küsi backendilt VIIMAST salvestatud küsimust,
   * et saaksime treeneri juures jätkata sealt, kus pooleli jäi.
   *
   * Kui midagi ei ole, backend tagastab 204 No Content.
   */
  getLastQuestion(email: string): Observable<AdaptiveQuestionResponse> {
    const params = new HttpParams().set('email', email);
    return this.http.get<AdaptiveQuestionResponse>(
      `${this.baseUrl}/last-question`,
      { params, observe: 'body' as const }
    );
  }
}
