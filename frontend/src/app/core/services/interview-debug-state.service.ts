import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface InterviewDebugSessionInfo {
  sessionId?: string | number | null;
  sessionUuid?: string | null;
  email?: string | null;
}

export interface InterviewDebugState {
  sessionInfo: InterviewDebugSessionInfo | null;
  lastRequest: unknown | null;
  lastResponse: unknown | null;
  lastError: unknown | null;
  isDrawerOpen: boolean;
}

@Injectable({ providedIn: 'root' })
export class InterviewDebugStateService {
  private readonly STORAGE_PREFIX = 'ai-interview-debug:';

  private readonly stateSubject = new BehaviorSubject<InterviewDebugState>({
    sessionInfo: null,
    lastRequest: null,
    lastResponse: null,
    lastError: null,
    isDrawerOpen: false
  });

  readonly state$ = this.stateSubject.asObservable();

  setSessionInfo(info: InterviewDebugSessionInfo): void {
    const current = this.stateSubject.value;
    const mergedInfo: InterviewDebugSessionInfo = {
      ...current.sessionInfo,
      ...info
    };

    const loaded = this.loadFromStorage(mergedInfo.sessionUuid || null);
    const next: InterviewDebugState = loaded
      ? {
          ...loaded,
          sessionInfo: {
            ...(loaded.sessionInfo || {}),
            ...mergedInfo
          }
        }
      : {
          ...current,
          sessionInfo: mergedInfo
        };

    this.stateSubject.next(next);
    this.persist(next);
  }

  setDrawerOpen(isOpen: boolean): void {
    this.patchState({ isDrawerOpen: isOpen });
  }

  toggleDrawer(): void {
    const current = this.stateSubject.value;
    this.setDrawerOpen(!current.isDrawerOpen);
  }

  setLastRequest(sessionUuid: string | null, payload: unknown): void {
    this.patchState({ lastRequest: payload }, sessionUuid);
  }

  setLastResponse(sessionUuid: string | null, payload: unknown): void {
    this.patchState({ lastResponse: payload, lastError: null }, sessionUuid);
  }

  setLastError(sessionUuid: string | null, error: unknown): void {
    this.patchState({ lastError: error }, sessionUuid);
  }

  private patchState(
    patch: Partial<InterviewDebugState>,
    explicitSessionUuid: string | null = null
  ): void {
    const current = this.stateSubject.value;
    const next: InterviewDebugState = {
      ...current,
      ...patch
    };
    this.stateSubject.next(next);

    const sessionUuid =
      explicitSessionUuid ?? next.sessionInfo?.sessionUuid ?? null;
    this.persist(next, sessionUuid);
  }

  private storageKey(sessionUuid: string): string {
    return `${this.STORAGE_PREFIX}${sessionUuid}`;
  }

  private loadFromStorage(
    sessionUuid: string | null
  ): InterviewDebugState | null {
    if (!sessionUuid) {
      return null;
    }
    try {
      const raw = localStorage.getItem(this.storageKey(sessionUuid));
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        return null;
      }
      return {
        sessionInfo: parsed.sessionInfo ?? null,
        lastRequest: parsed.lastRequest ?? null,
        lastResponse: parsed.lastResponse ?? null,
        lastError: parsed.lastError ?? null,
        isDrawerOpen: !!parsed.isDrawerOpen
      } as InterviewDebugState;
    } catch {
      return null;
    }
  }

  private persist(
    state: InterviewDebugState,
    explicitSessionUuid: string | null = null
  ): void {
    const sessionUuid =
      explicitSessionUuid ?? state.sessionInfo?.sessionUuid ?? null;
    if (!sessionUuid) {
      return;
    }
    try {
      const payload = {
        sessionInfo: state.sessionInfo,
        lastRequest: state.lastRequest,
        lastResponse: state.lastResponse,
        lastError: state.lastError,
        isDrawerOpen: state.isDrawerOpen
      };
      localStorage.setItem(
        this.storageKey(sessionUuid),
        JSON.stringify(payload)
      );
    } catch {
      // ignore storage failures
    }
  }
}


