import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdaptiveAnalysisRequest,
  AdaptiveAnalysisResponse,
} from './ai-feedback.model';

@Injectable({ providedIn: 'root' })
export class AiFeedbackService {
  private readonly baseUrl = '/api/trainer/analyze';

  constructor(private http: HttpClient) {}

  analyzeAnswer(
    payload: AdaptiveAnalysisRequest
  ): Observable<AdaptiveAnalysisResponse> {
    return this.http.post<AdaptiveAnalysisResponse>(this.baseUrl, payload);
  }
}
