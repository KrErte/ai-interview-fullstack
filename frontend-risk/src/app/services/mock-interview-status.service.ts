import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, shareReplay, timeout } from 'rxjs';
import { environment } from '../../environments/environment';

export type MockProvider = 'anthropic' | 'openai';

/**
 * Whether the AI mock interview is on and which language model scores it (GET /api/ai-readiness/mock-interview/status).
 * One request per app session. Fails closed: an error, a 5 s timeout or an unknown value counts as off.
 */
@Injectable({ providedIn: 'root' })
export class MockInterviewStatusService {
  static readonly TIMEOUT_MS = 5000;
  private provider$?: Observable<MockProvider | null>;

  constructor(private http: HttpClient) {}

  provider(): Observable<MockProvider | null> {
    if (!this.provider$) {
      this.provider$ = this.http.get<{ provider?: unknown }>(`${environment.apiBaseUrl}/api/ai-readiness/mock-interview/status`).pipe(
        timeout(MockInterviewStatusService.TIMEOUT_MS),
        map(s => (s?.provider === 'anthropic' || s?.provider === 'openai' ? s.provider : null)),
        catchError(() => of(null)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.provider$;
  }

  /** The name shown to people: who receives the answer. */
  static label(provider: MockProvider | null): string {
    return provider === 'anthropic' ? 'Anthropic (Claude)' : provider === 'openai' ? 'OpenAI' : '';
  }
}
