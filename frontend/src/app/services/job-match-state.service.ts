import { Injectable } from '@angular/core';
import { JobMatchResult } from './job-match.service';

@Injectable({
  providedIn: 'root'
})
export class JobMatchStateService {
  private lastResult: JobMatchResult | null = null;
  private trainingSessionId: string | null = null;

  setLastResult(result: JobMatchResult): void {
    this.lastResult = result;
  }

  getLastResult(): JobMatchResult | null {
    return this.lastResult;
  }

  setCurrentTrainingSessionId(id: string | null): void {
    this.trainingSessionId = id;
  }

  getCurrentTrainingSessionId(): string | null {
    return this.trainingSessionId;
  }
}

