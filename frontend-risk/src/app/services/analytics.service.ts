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
