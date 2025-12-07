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
  token: string;
  email?: string;
  fullName?: string;
  userRole?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'aiim_token';
  private readonly EMAIL_KEY = 'aiim_email';
  private readonly NAME_KEY = 'aiim_name';
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;


  constructor(private http: HttpClient) {}

  register(payload: RegisterPayload): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, payload)
      .pipe(
        tap((res) => this.persistSession(res, payload.email, payload.fullName)),
        map(() => void 0)
      );
  }

  login(payload: LoginPayload): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, payload)
      .pipe(
        tap((res) => this.persistSession(res, payload.email)),
        map(() => void 0)
      );
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentUserEmail(): string | null {
    return localStorage.getItem(this.EMAIL_KEY);
  }

  getCurrentUserName(): string | null {
    return localStorage.getItem(this.NAME_KEY);
  }

  persistSession(res: AuthResponse, fallbackEmail?: string, fallbackName?: string) {
    if (!res?.token) {
      return;
    }
    localStorage.setItem(this.TOKEN_KEY, res.token);
    const email = res.email || fallbackEmail;
    const name = res.fullName || fallbackName;

    if (email) {
      localStorage.setItem(this.EMAIL_KEY, email);
    }
    if (name) {
      localStorage.setItem(this.NAME_KEY, name);
    }
  }

  setDisplayName(name: string) {
    localStorage.setItem(this.NAME_KEY, name);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    localStorage.removeItem(this.NAME_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
