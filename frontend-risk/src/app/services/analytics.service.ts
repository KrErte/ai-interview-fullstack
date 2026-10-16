import { Injectable } from '@angular/core';

type EventData = Record<string, string | number>;

interface Umami {
  track(event: string, data?: EventData): void;
}

/**
 * Custom events for Umami (loaded in index.html, production host only).
 * Page views are tracked by Umami itself, including SPA navigation.
 * No-op when there is no window (SSR / tests), when the script is blocked or not loaded,
 * or when it throws, so analytics never breaks the app.
 */
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  static readonly PURCHASE_KEY_PREFIX = 'fp_purchase_tracked:';

  /**
   * Sends 'purchase' once per checkout session id (from the return URL's session_id; a fallback key when it is
   * missing), deduped in sessionStorage. No-op without Umami. Returns true when the event was sent.
   */
  trackPurchaseOnce(sessionId: string | null | undefined, data?: EventData): boolean {
    const id = typeof sessionId === 'string' && sessionId.trim() ? sessionId.trim().slice(0, 200) : 'no-session-id';
    const key = AnalyticsService.PURCHASE_KEY_PREFIX + id;
    try {
      if (sessionStorage.getItem(key)) return false;
      const umami = typeof window === 'undefined' ? undefined : (window as unknown as { umami?: Umami }).umami;
      if (!umami || typeof umami.track !== 'function') return false;
      sessionStorage.setItem(key, '1');
    } catch {
      return false;
    }
    this.track('purchase', data);
    return true;
  }

  track(event: string, data?: EventData): void {
    try {
      if (typeof window === 'undefined') {
        return;
      }
      const umami = (window as unknown as { umami?: Umami }).umami;
      if (umami && typeof umami.track === 'function') {
        umami.track(event, data);
      }
    } catch {
      // analytics must never affect the user
    }
  }
}
