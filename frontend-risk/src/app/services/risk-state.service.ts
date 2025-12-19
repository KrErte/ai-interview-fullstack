import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { RiskAssessment, RiskState } from '../models/risk.model';

@Injectable({
  providedIn: 'root'
})
export class RiskStateService {
  private readonly initialState: RiskState = {
    assessmentReady: false,
    loading: false,
    error: null,
    assessment: null
  };

  private stateSubject = new BehaviorSubject<RiskState>(this.initialState);
  state$: Observable<RiskState> = this.stateSubject.asObservable();

  get currentState(): RiskState {
    return this.stateSubject.value;
  }

  setLoading(loading: boolean): void {
    this.stateSubject.next({
      ...this.currentState,
      loading,
      error: loading ? null : this.currentState.error
    });
  }

  setAssessment(assessment: RiskAssessment): void {
    this.stateSubject.next({
      ...this.currentState,
      assessmentReady: true,
      loading: false,
      error: null,
      assessment
    });
  }

  setError(error: string): void {
    this.stateSubject.next({
      ...this.currentState,
      loading: false,
      error
    });
  }

  reset(): void {
    this.stateSubject.next(this.initialState);
  }

  /**
   * Format confidence as percentage with 1 decimal
   * e.g., 0.699 -> "69.9%"
   */
  formatConfidence(confidence: number): string {
    return `${(confidence * 100).toFixed(1)}%`;
  }
}
