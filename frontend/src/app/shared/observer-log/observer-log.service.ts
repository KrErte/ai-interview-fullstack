import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Toggle for demo logging.
 * NOTE: Keep this constant defined ONLY ONCE in the whole project to avoid TS2451.
 */
export const DEMO_OBSERVER_LOG = true;

export type ObserverEventLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

/**
 * Keep this UNION backward compatible with older panel code.
 * (Old panel uses: START, SUBMIT, NEXT_Q, SIGNAL, ERROR)
 */
export type ObserverEventType =
  | 'UI'
  | 'API'
  | 'STATE'
  | 'DECISION'
  | 'SCORING'
  | 'PERSISTENCE'
  | 'GUARD'
  | 'OTHER'
  | 'START'
  | 'SUBMIT'
  | 'NEXT_Q'
  | 'SIGNAL'
  | 'ERROR';

export interface ObserverLogEvent {
  id: string;
  ts: string; // ISO timestamp
  level: ObserverEventLevel;
  type: ObserverEventType;
  message: string;

  // Optional structured context
  sessionUuid?: string;
  meta?: Record<string, unknown>;
}

/**
 * Backward compatible alias (your panel imports ObserverLogEntry).
 */
export type ObserverLogEntry = ObserverLogEvent;

export interface ObserverLogOptions {
  enabled?: boolean;
  maxEntries?: number;
}

@Injectable({ providedIn: 'root' })
export class ObserverLogService {
  private enabled = DEMO_OBSERVER_LOG;
  private maxEntries = 200;

  private readonly _events$ = new BehaviorSubject<ObserverLogEvent[]>([]);
  /** Stream for UI (new API) */
  readonly events$: Observable<ObserverLogEvent[]> = this._events$.asObservable();

  constructor() {}

  configure(opts: ObserverLogOptions): void {
    if (typeof opts.enabled === 'boolean') this.enabled = opts.enabled;
    if (typeof opts.maxEntries === 'number' && opts.maxEntries > 0) this.maxEntries = opts.maxEntries;
  }

  clear(): void {
    this._events$.next([]);
  }

  /**
   * Backward compatible getter (old panel calls get$()).
   */
  get$(): Observable<ObserverLogEvent[]> {
    return this.events$;
  }

  log(
    type: ObserverEventType,
    message: string,
    meta?: Record<string, unknown>,
    level: ObserverEventLevel = 'INFO',
    sessionUuid?: string
  ): void {
    if (!this.enabled) return;

    const next: ObserverLogEvent = {
      id: this.uuid(),
      ts: new Date().toISOString(),
      level,
      type,
      message,
      sessionUuid,
      meta,
    };

    const current = this._events$.getValue();
    const appended = [next, ...current]; // newest first
    this._events$.next(appended.slice(0, this.maxEntries));
  }

  debug(type: ObserverEventType, message: string, meta?: Record<string, unknown>, sessionUuid?: string): void {
    this.log(type, message, meta, 'DEBUG', sessionUuid);
  }

  info(type: ObserverEventType, message: string, meta?: Record<string, unknown>, sessionUuid?: string): void {
    this.log(type, message, meta, 'INFO', sessionUuid);
  }

  warn(type: ObserverEventType, message: string, meta?: Record<string, unknown>, sessionUuid?: string): void {
    this.log(type, message, meta, 'WARN', sessionUuid);
  }

  error(type: ObserverEventType, message: string, meta?: Record<string, unknown>, sessionUuid?: string): void {
    this.log(type, message, meta, 'ERROR', sessionUuid);
  }

  api(message: string, meta?: Record<string, unknown>, level: ObserverEventLevel = 'INFO', sessionUuid?: string): void {
    this.log('API', message, meta, level, sessionUuid);
  }

  decision(message: string, meta?: Record<string, unknown>, level: ObserverEventLevel = 'INFO', sessionUuid?: string): void {
    this.log('DECISION', message, meta, level, sessionUuid);
  }

  private uuid(): string {
    const g = globalThis as any;
    if (g?.crypto?.randomUUID) return g.crypto.randomUUID();

    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }
}
