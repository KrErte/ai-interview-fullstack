// src/app/services/personality.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  PersonalityInterviewStartResponse,
  PersonalityAnswerRequest,
  PersonalityProfile,
  PersonalityCompatibilityRequest,
  PersonalityCompatibilityResponse
} from '../models/personality.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PersonalityService {
  private readonly baseUrl = '/api/personality';

  constructor(private http: HttpClient) {}

  startInterview(email: string): Observable<PersonalityInterviewStartResponse> {
    return this.http.post<PersonalityInterviewStartResponse>(
      `${this.baseUrl}/interview/start`,
      { email }
    );
  }

  submitAnswers(payload: PersonalityAnswerRequest): Observable<PersonalityProfile> {
    return this.http.post<PersonalityProfile>(
      `${this.baseUrl}/interview/answer`,
      payload
    );
  }

  getProfile(email: string): Observable<PersonalityProfile> {
    return this.http.get<PersonalityProfile>(
      `${this.baseUrl}/profile/${encodeURIComponent(email)}`);
  }

  getCompatibility(
    request: PersonalityCompatibilityRequest
  ): Observable<PersonalityCompatibilityResponse> {
    return this.http.post<PersonalityCompatibilityResponse>(
      `${this.baseUrl}/compatibility`,
      request
    );
  }
}
