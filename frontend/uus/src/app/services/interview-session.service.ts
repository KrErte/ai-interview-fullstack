// src/app/services/interview-session.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class InterviewSessionService {
  private apiUrl = 'http://localhost:8080/api/interview-sessions';  // <-- PARANDATUD!

  constructor(private http: HttpClient) {}

  createSession(payload: any): Observable<any> {
    return this.http.post(this.apiUrl, payload);  // <--- /api/create-session EI OLE ENAM!
  }
}
