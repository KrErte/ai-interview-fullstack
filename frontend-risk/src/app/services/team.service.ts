import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreatedTeam, TeamInvite, TeamReport } from '../models/team.model';

/**
 * Team report without an account. The report token is the only key; it lives in the URL fragment
 * (#token), which browsers never send to servers or analytics.
 */
@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/teams`;
  /** Team code -> edit token of this browser's anonymous answer (soft guard against double answers). */
  private readonly ANSWERS_KEY = 'ai_readiness_teams';

  constructor(private http: HttpClient) {}

  create(name: string, ownerEmail: string): Observable<CreatedTeam> {
    return this.http.post<CreatedTeam>(this.baseUrl, { name, ownerEmail });
  }

  join(code: string): Observable<TeamInvite> {
    return this.http.get<TeamInvite>(`${this.baseUrl}/join/${encodeURIComponent(code)}`);
  }

  report(token: string): Observable<TeamReport> {
    return this.http.get<TeamReport>(`${this.baseUrl}/report/${encodeURIComponent(token)}`);
  }

  delete(token: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/report/${encodeURIComponent(token)}`);
  }

  /** Team codes are case-insensitive; the server stores them in upper case. */
  static normalizeCode(code: string | null | undefined): string | null {
    const c = (code ?? '').trim().toUpperCase();
    return /^[A-Z2-7]{10}$/.test(c) ? c : null;
  }

  /** This browser's edit token for its answer in a team, if it already answered. */
  editToken(code: string): string | null {
    return this.read()[code] ?? null;
  }

  rememberAnswer(code: string, editToken: string): void {
    const all = this.read();
    all[code] = editToken;
    try {
      localStorage.setItem(this.ANSWERS_KEY, JSON.stringify(all));
    } catch {
      // storage unavailable – the soft double-answer guard just does not work in this browser
    }
  }

  private read(): Record<string, string> {
    try {
      const raw = localStorage.getItem(this.ANSWERS_KEY);
      const parsed = raw ? JSON.parse(raw) : null;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
    } catch {
      return {};
    }
  }
}
