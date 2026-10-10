import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toFiniteNumber, clampPercent, roundPercent } from '../../services/risk.service';

/**
 * Qualitative bands for the CV-to-job match score (0-100).
 * Higher score = better match with the job description.
 */
export type JobMatchBand = 'strong' | 'partial' | 'weak';

export const STRONG_MATCH_THRESHOLD = 70;
export const PARTIAL_MATCH_THRESHOLD = 40;

export function getJobMatchBand(matchPercent: number): JobMatchBand {
  if (matchPercent >= STRONG_MATCH_THRESHOLD) return 'strong';
  if (matchPercent >= PARTIAL_MATCH_THRESHOLD) return 'partial';
  return 'weak';
}

const JOB_MATCH_LABELS: Record<JobMatchBand, string> = {
  strong: 'Strong match',
  partial: 'Partial match',
  weak: 'Weak match'
};

/** Green = good match, amber = partial, red = weak. */
const JOB_MATCH_COLOR_CLASSES: Record<JobMatchBand, string> = {
  strong: 'text-emerald-400',
  partial: 'text-amber-400',
  weak: 'text-rose-400'
};

export function getJobMatchLabel(matchPercent: number): string {
  return JOB_MATCH_LABELS[getJobMatchBand(matchPercent)];
}

export function getJobMatchColorClass(matchPercent: number): string {
  return JOB_MATCH_COLOR_CLASSES[getJobMatchBand(matchPercent)];
}

@Component({
  selector: 'app-snapshot-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-3xl border border-slate-800/50 bg-gradient-to-br from-slate-900/80 to-slate-900/40 p-8 backdrop-blur-sm card-hover">
      <!-- Header -->
      <div class="flex items-center gap-3 mb-8">
        <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
        </div>
        <div>
          <h2 class="text-xl font-semibold text-slate-100">Job Match Snapshot</h2>
          <p class="text-sm text-slate-500">How well your CV matches this job description</p>
        </div>
      </div>

      <!-- Main Score Display -->
      <div class="flex flex-col sm:flex-row items-center justify-between gap-8 mb-8">
        <!-- Job Match Score -->
        <div class="text-center sm:text-left">
          <p class="text-sm font-medium text-slate-400 uppercase tracking-wider mb-2">Job match</p>
          <ng-container *ngIf="hasScore(); else noScore">
            <div
              class="flex items-baseline gap-1"
              role="img"
              [attr.aria-label]="'Job match: ' + displayPercent() + '%, ' + getMatchLabel()"
            >
              <span class="text-6xl font-bold" [ngClass]="getMatchColorClass()" aria-hidden="true">{{ displayPercent() }}</span>
              <span class="text-3xl font-semibold text-slate-500" aria-hidden="true">%</span>
            </div>
            <p class="text-sm text-slate-500 mt-2">{{ getMatchLabel() }}</p>
          </ng-container>
          <ng-template #noScore>
            <div
              class="flex items-baseline gap-1"
              role="img"
              aria-label="Job match: not available"
            >
              <span class="text-6xl font-bold text-slate-500" aria-hidden="true">&mdash;</span>
            </div>
            <p class="text-sm text-slate-500 mt-2 max-w-xs">
              We couldn't calculate a match for this job.
            </p>
          </ng-template>
        </div>

        <!-- Divider -->
        <div class="hidden sm:block w-px h-24 bg-gradient-to-b from-transparent via-slate-700 to-transparent"></div>

        <!-- Confidence -->
        <div class="text-center sm:text-left">
          <p class="text-sm font-medium text-slate-400 uppercase tracking-wider mb-2">Confidence</p>
          <div class="flex items-baseline gap-1">
            <span class="text-5xl font-bold text-slate-200">{{ formatConfidence() }}</span>
          </div>
          <p class="text-sm text-slate-500 mt-2">Based on {{ signalsCount }} signals</p>
        </div>
      </div>

      <!-- Coverage Bar -->
      <div class="space-y-3">
        <div class="flex items-center justify-between text-sm">
          <span class="text-slate-400">Analysis Coverage</span>
          <span class="font-medium text-slate-300">{{ coverage | number:'1.0-0' }}%</span>
        </div>
        <div class="h-2 rounded-full bg-slate-800 overflow-hidden">
          <div
            class="h-full rounded-full bg-gradient-to-r from-emerald-500 to-teal-400 animate-progress"
            [style.width.%]="coverage"
          ></div>
        </div>
        <p class="text-xs text-slate-500">
          Higher coverage means a more accurate job match and roadmap recommendations.
        </p>
      </div>
    </div>
  `
})
export class SnapshotCardComponent {
  /**
   * CV-to-job match score (0-100); higher is better.
   * null/undefined/NaN/non-numeric = no score (empty state); 0 is a real score.
   * Uses the same finite-number check as RiskService (toFiniteNumber).
   */
  @Input() matchPercent: number | null | undefined = null;
  @Input() confidence = 0;
  @Input() signalsCount = 0;
  @Input() coverage = 0;

  formatConfidence(): string {
    return `${(this.confidence * 100).toFixed(1)}%`;
  }

  /** True only for a finite number (or numeric string), so a genuine 0 counts as a score. */
  hasScore(): boolean {
    return toFiniteNumber(this.matchPercent) !== null;
  }

  /**
   * Rounded, 0-100 clamped score used for both the number and the band, so they agree.
   * matchPercent is already on the 0-100 scale (RiskService.toMatchPercent), so
   * no x100 here. Uses the same roundPercent() as the service, so a non-integer
   * input with float noise (28.499999999999996) still shows 29, and an integer
   * from the service passes through unchanged.
   */
  displayPercent(): number {
    return clampPercent(roundPercent(toFiniteNumber(this.matchPercent) ?? 0));
  }

  getMatchColorClass(): string {
    return getJobMatchColorClass(this.displayPercent());
  }

  getMatchLabel(): string {
    return getJobMatchLabel(this.displayPercent());
  }
}
