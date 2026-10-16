import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { RiskStateService } from './risk-state.service';

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
  private readonly TOKEN_KEY = 'risk_token';
  private readonly EMAIL_KEY = 'risk_email';
  private readonly NAME_KEY = 'risk_name';
  // CV text belongs to the account. The anonymous test result stays in the browser across logout
  // (the privacy policy says so) and is removed only when a different person signs in.
  private readonly ACCOUNT_DATA_KEYS = ['risk_cv_text'];
  private readonly RESULT_KEY = 'ai_readiness_last_result';
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  constructor(
    private http: HttpClient,
    private riskState: RiskStateService
  ) {}

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

  persistSession(res: AuthResponse, fallbackEmail?: string, fallbackName?: string): void {
    if (!res?.token) {
      return;
    }
    const email = res.email || fallbackEmail;
    const name = res.fullName || fallbackName;
    const previousEmail = this.getCurrentUserEmail();
    if (previousEmail && previousEmail.toLowerCase() !== (email || '').toLowerCase()) {
      this.clearAccountData();
      this.clearLastResult();
    }
    localStorage.setItem(this.TOKEN_KEY, res.token);

    if (email) {
      localStorage.setItem(this.EMAIL_KEY, email);
    }
    if (name) {
      localStorage.setItem(this.NAME_KEY, name);
    }
  }

  setDisplayName(name: string): void {
    localStorage.setItem(this.NAME_KEY, name);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    localStorage.removeItem(this.NAME_KEY);
    this.clearAccountData();
  }

  private clearAccountData(): void {
    this.ACCOUNT_DATA_KEYS.forEach(key => localStorage.removeItem(key));
    this.riskState.reset();
  }

  private clearLastResult(): void {
    localStorage.removeItem(this.RESULT_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
