// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
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
  email: string;
  fullName: string;
  userRole: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'authToken';
  private readonly EMAIL_KEY = 'authEmail';
  private readonly NAME_KEY = 'authFullName';
  private readonly ROLE_KEY = 'authUserRole';
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  register(payload: RegisterPayload): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, payload)
      .pipe(
        tap((res) => this.saveAuth(res, payload.email, payload.fullName)),
        map(() => void 0)
      );
  }

  /**
   * Supports both (email, password) and payload usage to keep legacy callers working.
   */
  login(email: string, password: string): Observable<AuthResponse>;
  login(payload: LoginPayload): Observable<AuthResponse>;
  login(emailOrPayload: string | LoginPayload, password?: string): Observable<AuthResponse> {
    const body = typeof emailOrPayload === 'string'
      ? { email: emailOrPayload, password: password ?? '' }
      : emailOrPayload;

    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, body)
      .pipe(tap((res) => this.saveAuth(res, body.email)));
  }

  saveAuth(res: AuthResponse, fallbackEmail?: string, fallbackName?: string): void {
    if (!res?.token) {
      return;
    }

    localStorage.setItem(this.TOKEN_KEY, res.token);

    const email = res.email || fallbackEmail;
    if (email) {
      localStorage.setItem(this.EMAIL_KEY, email);
    }

    const fullName = res.fullName || fallbackName;
    if (fullName) {
      localStorage.setItem(this.NAME_KEY, fullName);
    }

    if (res.userRole) {
      localStorage.setItem(this.ROLE_KEY, res.userRole);
    }
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

  getCurrentUserRole(): string | null {
    return localStorage.getItem(this.ROLE_KEY);
  }

  setDisplayName(name: string): void {
    localStorage.setItem(this.NAME_KEY, name);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    localStorage.removeItem(this.NAME_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
