import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CvQuestion {
  id: string;
  question: string;
  category: string;
  difficulty: string;
}

export interface GenerateQuestionsRequest {
  cvText: string;
  technicalCount?: number;
  softCount?: number;
}

export interface CvTextResponse {
  text: string;
}

/** Payload vastuse hindamiseks  */
export interface EvaluateAnswerPayload {
  email: string;
  question: string;
  answer: string;
}

/** Backendist tulev hindamise vastus */
export interface EvaluateAnswerResponse {
  strengths: string;
  weaknesses: string;
  suggestions: string;
  score: number;
}

@Injectable({
  providedIn: 'root'
})
export class CvQuestionService {

  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  generateQuestions(req: GenerateQuestionsRequest): Observable<CvQuestion[]> {
    return this.http.post<CvQuestion[]>(
      `${this.baseUrl}/questions`,
      {
        cvText: req.cvText,
        technicalCount: req.technicalCount ?? 6,
        softCount: req.softCount ?? 4
      }
    );
  }

  uploadCvPdf(file: File): Observable<CvTextResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CvTextResponse>(
      `${this.baseUrl}/cv/extract-text`,
      formData
    );
  }

  /**
   * Kutsub backendis vastuse hindamise endpointi.
   * Kui sinu controlleri URL on teistsugune, muuda ainult `${this.baseUrl}/answers/evaluate` osa.
   */
  evaluateAnswer(payload: EvaluateAnswerPayload): Observable<EvaluateAnswerResponse> {
    return this.http.post<EvaluateAnswerResponse>(
      `${this.baseUrl}/answers/evaluate`,
      payload
    );
  }
}
