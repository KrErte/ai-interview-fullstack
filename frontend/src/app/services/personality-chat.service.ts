// src/app/services/personality-chat.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PersonalityTurn,
  PersonalityQuestionResponse,
} from '../models/personality-question.model';

@Injectable({
  providedIn: 'root',
})
export class PersonalityChatService {
  // saad soovi korral muuta: nt '/api/personality'
  private readonly baseUrl = '/api/personality-interview';

  constructor(private http: HttpClient) {}

  /**
   * Käivitab uue isiksuse / tööstiili intervjuu.
   * Backendis võiks see luua uue GPT sessiooni.
   */
  startInterview(email: string | null): Observable<PersonalityQuestionResponse> {
    return this.http.post<PersonalityQuestionResponse>(`${this.baseUrl}/start`, {
      email,
    });
  }

  /**
   * Saadab kasutaja vastuse + senise ajaloo, backend tagastab järgmise küsimuse.
   */
  answerQuestion(payload: {
    email: string | null;
    question: string;
    answer: string;
    history: PersonalityTurn[];
  }): Observable<PersonalityQuestionResponse> {
    return this.http.post<PersonalityQuestionResponse>(
      `${this.baseUrl}/answer`,
      payload
    );
  }
}
