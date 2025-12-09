// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
}

export type AuthResponse = LoginResponse;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly EMAIL_KEY = 'auth_email';
  private readonly NAME_KEY = 'auth_fullName';
  private readonly ROLE_KEY = 'auth_role';
  private readonly authUrl = `${environment.apiUrl}/api/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  register(payload: RegisterPayload): Observable<LoginResponse> {
    return this.http
      .post<AuthResponse>(`${this.authUrl}/register`, payload)
      .pipe(tap((res) => this.saveAuth(res, payload.email, payload.fullName)));
  }

  /**
   * Login with email and password.
   * POSTs to /auth/login with { email, password }
   */
  login(payload: LoginRequest): Observable<LoginResponse> {
    const body = { email: payload.email, password: payload.password };

    return this.http
      .post<AuthResponse>(`${this.authUrl}/login`, body)
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

    if (res.role) {
      localStorage.setItem(this.ROLE_KEY, res.role);
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
