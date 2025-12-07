import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StarAnswerService {

  private readonly apiUrl = `${environment.apiBaseUrl}/star`;

  constructor(private http: HttpClient) {}

  generate(
    question: string,
    cvText: string,
    jobDescription: string
  ): Observable<{ answer: string }> {
    return this.http.post<{ answer: string }>(this.apiUrl, {
      question,
      cvText,
      jobDescription
    });
  }
}
