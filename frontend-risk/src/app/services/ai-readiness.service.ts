import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AssessmentRequest,
  AssessmentResult,
  CategorySummary,
  Features,
  FullRoadmap,
  PendingPurchase,
  RoadmapTeaser,
  TeaserPhaseView,
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
  static readonly PENDING_PURCHASE_KEY = 'ai_readiness_pending_purchase';

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
    // a locked result never carries the paid roadmaps into the app, whatever the backend sends
    return this.http.post<AssessmentResult>(`${this.baseUrl}/assess`, request).pipe(map(lockedResultView));
  }

  /**
   * The paid, full roadmap for these answers (auth only). 402 {error:'payment_required'} when the user
   * has not bought it. FP-64 polls the entitlement after payment and then calls this.
   */
  fullRoadmap(professionId: string, answers: Record<string, string>): Observable<FullRoadmap> {
    return this.http.post<FullRoadmap>(`${this.baseUrl}/roadmap`, { professionId, answers });
  }

  /** Keeps the answers across the login/register detour before checkout (this tab only). */
  savePendingPurchase(pending: PendingPurchase): void {
    try {
      sessionStorage.setItem(AiReadinessService.PENDING_PURCHASE_KEY, JSON.stringify(pending));
    } catch {
      // storage unavailable – the user retakes the test after login
    }
  }

  loadPendingPurchase(): PendingPurchase | null {
    try {
      const raw = sessionStorage.getItem(AiReadinessService.PENDING_PURCHASE_KEY);
      const p = raw ? (JSON.parse(raw) as PendingPurchase) : null;
      return p && typeof p.professionId === 'string' && p.answers && typeof p.answers === 'object' ? p : null;
    } catch {
      return null;
    }
  }

  clearPendingPurchase(): void {
    try {
      sessionStorage.removeItem(AiReadinessService.PENDING_PURCHASE_KEY);
    } catch {
      // ignore
    }
  }

  /**
   * Remembers the result and, when given, the answers (and the user's own task shares) behind it,
   * so a saved result can later be recalculated with the current catalog (see loadLastAnswers).
   */
  saveLastResult(result: AssessmentResult, answers?: Record<string, string>, taskShares?: Record<string, number>,
                 teamCode?: string): void {
    try {
      // the team status (with its edit token) belongs to TeamService storage, not to the saved result
      const { team: _team, ...withoutTeam } = lockedResultView(result);
      // unpaid (roadmapLocked): only the teaser is stored; this also overwrites a stale full roadmap
      localStorage.setItem(this.RESULT_KEY, JSON.stringify(withoutTeam));
      if (result.roadmapLocked || result.billingEnabled) {
        // ticked steps are roadmap step texts, so they are dropped for a locked profession too
        this.saveProgress(result.professionId, []);
      }
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
      const saved = raw ? (JSON.parse(raw) as AssessmentResult) : null;
      if ((saved?.roadmapLocked || saved?.billingEnabled) && hasFullRoadmap(saved)) {
        // stale full roadmap stored for a locked result (older build): clear it in storage too
        const clean = lockedResultView(saved);
        localStorage.setItem(this.RESULT_KEY, JSON.stringify(clean));
        return clean;
      }
      return saved;
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
    // unpaid: no "Continue your plan" entry point
    const phases = result && !result.roadmapLocked && !result.billingEnabled ? result.professionRoadmap ?? [] : [];
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

/** The teaser as display lines, whatever shape the backend sends. */
export function roadmapTeaserLines(teaser: RoadmapTeaser | null | undefined): string[] {
  if (!teaser) return [];
  if (typeof teaser === 'string') return teaser.trim() ? [teaser.trim()] : [];
  if (!Array.isArray(teaser)) return [];
  return teaser
    .map(t => (typeof t === 'string' ? t : t && typeof t.title === 'string' ? t.title : ''))
    .map(t => t.trim())
    .filter(t => !!t);
}

function hasFullRoadmap(r: AssessmentResult): boolean {
  return !!(r.generalRoadmap?.length || r.professionRoadmap?.length);
}

/**
 * A locked (unpaid) result without the paid roadmaps: only the teaser is kept. The full plan for paying
 * users comes from fullRoadmap() and is held in memory only. Results that are not locked pass unchanged.
 */
export function lockedResultView(r: AssessmentResult): AssessmentResult {
  if (!r || !(r.roadmapLocked || r.billingEnabled)) return r;
  return { ...r, generalRoadmap: [], professionRoadmap: [], totalWeeks: 0 };
}

/**
 * The phased teaser ([{phase:1,title,items,project},{phase:2,title},{phase:3,title}]) as views; null when the
 * teaser is in one of the old shapes (use roadmapTeaserLines then). Phase 1 is open; later phases are locked
 * and never carry items or a project, even if the backend sent them.
 */
export function roadmapTeaserPhases(teaser: RoadmapTeaser | null | undefined): TeaserPhaseView[] | null {
  if (!Array.isArray(teaser)) return null;
  const phased = teaser.filter((t): t is Exclude<typeof t, string> =>
    !!t && typeof t === 'object' && typeof t.phase === 'number' && typeof t.title === 'string' && !!t.title.trim());
  if (!phased.length) return null;
  return [...phased].sort((a, b) => a.phase! - b.phase!).map(t => {
    const locked = t.phase !== 1;
    return {
      phase: t.phase!,
      title: t.title!.trim(),
      items: locked || !Array.isArray(t.items) ? [] : t.items.filter(i => typeof i === 'string' && !!i.trim()),
      project: !locked && typeof t.project === 'string' && t.project.trim() ? t.project.trim() : null,
      locked
    };
  });
}
