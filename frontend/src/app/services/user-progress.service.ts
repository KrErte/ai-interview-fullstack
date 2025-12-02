// src/app/services/user-progress.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProgress } from '../models/user-progress.model';

@Injectable({
  providedIn: 'root',
})
export class UserProgressService {
  // PROXY -> backend: http://localhost:8080/api
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) {}

  getProgress(email: string): Observable<UserProgress> {
    const params = new HttpParams().set('email', email);
    return this.http.get<UserProgress>(`${this.baseUrl}/progress`, { params });
  }
}
