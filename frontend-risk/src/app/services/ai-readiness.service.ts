import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AssessmentRequest,
  AssessmentResult,
  CategorySummary,
  Features,
  ProfessionSummary,
  Question,
  SavedAnswers,
  SavedPlan
} from '../models/ai-readiness.model';

@Injectable({
  providedIn: 'root'
})
export class AiReadinessService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/ai-readiness`;
  private readonly RESULT_KEY = 'ai_readiness_last_result';
  private readonly ANSWERS_KEY = 'ai_readiness_last_answers';
  private readonly PROGRESS_KEY = 'ai_readiness_roadmap_progress';
  private readonly AI_MARKS_KEY = 'ai_readiness_ai_marks';

  constructor(private http: HttpClient) {}

  professions(): Observable<ProfessionSummary[]> {
    return this.http.get<ProfessionSummary[]>(`${this.baseUrl}/professions`);
  }

  /** Searches all ~900 professions by name and alternate title; an empty query with a category lists that group. */
  searchProfessions(query: string, category: string | null, limit = 50): Observable<ProfessionSummary[]> {
    let params = new HttpParams().set('q', query).set('limit', limit);
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<ProfessionSummary[]>(`${this.baseUrl}/professions/search`, { params });
  }

  /** One profession by id, e.g. to preselect it when arriving from a /jobs page. 404 for unknown ids. */
  profession(id: string): Observable<ProfessionSummary> {
    return this.http.get<ProfessionSummary>(`${this.baseUrl}/professions/${encodeURIComponent(id)}`);
  }

  features(): Observable<Features> {
    return this.http.get<Features>(`${this.baseUrl}/features`);
  }

  categories(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>(`${this.baseUrl}/professions/categories`);
  }

  questions(): Observable<Question[]> {
    return this.http.get<Question[]>(`${this.baseUrl}/questions`);
  }

  assess(request: AssessmentRequest): Observable<AssessmentResult> {
    return this.http.post<AssessmentResult>(`${this.baseUrl}/assess`, request);
  }

  /**
   * Remembers the result and, when given, the answers (and the user's own task shares) behind it,
   * so a saved result can later be recalculated with the current catalog (see loadLastAnswers).
   */
  saveLastResult(result: AssessmentResult, answers?: Record<string, string>, taskShares?: Record<string, number>,
                 teamCode?: string): void {
    try {
      // the team status (with its edit token) belongs to TeamService storage, not to the saved result
      const { team: _team, ...withoutTeam } = result;
      localStorage.setItem(this.RESULT_KEY, JSON.stringify(withoutTeam));
      if (answers) {
        const saved: SavedAnswers = { professionId: result.professionId, answers };
        if (taskShares) {
          saved.taskShares = taskShares;
        }
        if (teamCode) {
          saved.teamCode = teamCode;
        }
        localStorage.setItem(this.ANSWERS_KEY, JSON.stringify(saved));
      }
    } catch {
      // storage unavailable – result just won't be remembered
    }
  }

  /** The answers behind the saved result; null for results saved before answers were stored. */
  loadLastAnswers(): SavedAnswers | null {
    try {
      const raw = localStorage.getItem(this.ANSWERS_KEY);
      const saved = raw ? (JSON.parse(raw) as SavedAnswers) : null;
      return saved && typeof saved.professionId === 'string' && saved.answers ? saved : null;
    } catch {
      return null;
    }
  }

  loadLastResult(): AssessmentResult | null {
    try {
      const raw = localStorage.getItem(this.RESULT_KEY);
      return raw ? (JSON.parse(raw) as AssessmentResult) : null;
    } catch {
      return null;
    }
  }

  /**
   * Roadmap steps the user has ticked off for one profession. Steps are keyed by their text, so
   * a step that is reworded in the catalog simply starts unticked.
   */
  loadProgress(professionId: string): string[] {
    return this.readProgress()[professionId] ?? [];
  }

  saveProgress(professionId: string, done: string[]): void {
    const all = this.readProgress();
    if (done.length) {
      all[professionId] = done;
    } else {
      delete all[professionId];
    }
    try {
      localStorage.setItem(this.PROGRESS_KEY, JSON.stringify(all));
    } catch {
      // storage unavailable – ticks last only for this visit
    }
  }

  /**
   * The saved result's profession and how many of its roadmap steps (items and projects) are
   * ticked; null when nothing is saved or the result has no roadmap.
   */
  savedPlan(): SavedPlan | null {
    const result = this.loadLastResult();
    const phases = result?.professionRoadmap ?? [];
    if (!result || !phases.length) {
      return null;
    }
    const steps = phases.flatMap(ph => [...ph.items, ph.project]);
    const done = new Set(this.loadProgress(result.professionId));
    return {
      professionName: result.professionName,
      score: result.replaceabilityScore,
      done: steps.filter(step => done.has(step)).length,
      total: steps.length
    };
  }

  /**
   * Tasks the user marked "I already do this with AI", per profession. They only steer the
   * first-step suggestion and never change the score.
   */
  loadAiMarks(professionId: string): string[] {
    return this.readMap(this.AI_MARKS_KEY)[professionId] ?? [];
  }

  saveAiMarks(professionId: string, taskNames: string[]): void {
    const all = this.readMap(this.AI_MARKS_KEY);
    if (taskNames.length) {
      all[professionId] = taskNames;
    } else {
      delete all[professionId];
    }
    try {
      localStorage.setItem(this.AI_MARKS_KEY, JSON.stringify(all));
    } catch {
      // storage unavailable – marks last only for this visit
    }
  }

  private readProgress(): Record<string, string[]> {
    return this.readMap(this.PROGRESS_KEY);
  }

  private readMap(key: string): Record<string, string[]> {
    try {
      const raw = localStorage.getItem(key);
      const parsed = raw ? JSON.parse(raw) : null;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
    } catch {
      return {};
    }
  }

  clearLastResult(): void {
    try {
      localStorage.removeItem(this.RESULT_KEY);
      localStorage.removeItem(this.ANSWERS_KEY);
    } catch {
      // ignore
    }
  }
}
