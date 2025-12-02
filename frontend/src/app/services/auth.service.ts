// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string; // backendis AuthResponse sisaldab ainult tokenit
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'aiim_token';
  private readonly EMAIL_KEY = 'aiim_email';
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  register(payload: RegisterPayload): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, payload)
      .pipe(
        tap((res) => {
          // salvesta token + EMAIL ALATI
          localStorage.setItem(this.TOKEN_KEY, res.token);
          localStorage.setItem(this.EMAIL_KEY, payload.email);
        }),
        map(() => void 0)
      );
  }

  login(payload: LoginPayload): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, payload)
      .pipe(
        tap((res) => {
          // OLULINE FIX: kirjuta alati email üle
          localStorage.setItem(this.TOKEN_KEY, res.token);
          localStorage.setItem(this.EMAIL_KEY, payload.email);
        }),
        map(() => void 0)
      );
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentUserEmail(): string | null {
    return localStorage.getItem(this.EMAIL_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
