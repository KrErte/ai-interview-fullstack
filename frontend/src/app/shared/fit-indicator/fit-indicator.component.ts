import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-fit-indicator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fit-indicator.component.html',
  styleUrls: ['./fit-indicator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FitIndicatorComponent {
  /** 0..1 overall fit */
  @Input() overall: number | null | undefined = null;

  /** 0..1 current dimension fit */
  @Input() currentDimension: number | null | undefined = null;

  /** delta since previous overall, -1..+1 */
  @Input() trend: number | null | undefined = null;

  /** Whether backend has actually computed fit already */
  @Input() computed: boolean | null | undefined = null;

  /** number of answered questions used to gate visibility */
  @Input() questionCount: number | null | undefined = null;

  /** Optional label for dimension line, e.g. "ownership_accountability" */
  @Input() dimensionLabel: string | null | undefined = null;

  /** compact mode reduces padding/font and focuses on dimension line */
  @Input() compact: boolean = false;

  private toPercent01(v: number | null | undefined): number | null {
    if (v === null || v === undefined || Number.isNaN(v)) {
      return null;
    }
    const clamped = Math.max(0, Math.min(1, v));
    return Math.round(clamped * 100);
  }

  get overallPercent(): number | null {
    return this.toPercent01(this.overall);
  }

  get dimensionPercent(): number | null {
    if (this.computed !== true) {
      return null;
    }
    return this.toPercent01(this.currentDimension);
  }

  get showCalculating(): boolean {
    if (this.computed !== true) {
      return true;
    }
    return this.overallPercent === null;
  }

  get trendDir(): 'up' | 'down' | 'flat' {
    const t = this.trend ?? 0;
    if (t > 0.01) return 'up';
    if (t < -0.01) return 'down';
    return 'flat';
  }

  get trendSymbol(): string {
    const dir = this.trendDir;
    if (dir === 'up') return '▲';
    if (dir === 'down') return '▼';
    return '→';
  }

  get trendLabel(): string {
    if (this.computed !== true) {
      return 'FLAT';
    }
    const dir = this.trendDir;
    if (dir === 'up') return 'UP';
    if (dir === 'down') return 'DOWN';
    return 'FLAT';
  }

  get hasTrend(): boolean {
    return this.computed === true && this.trend !== null && this.trend !== undefined;
  }

  get overallBadgeClass(): string {
    const p = this.overallPercent;
    if (p === null) {
      return 'text-slate-300 border-slate-700';
    }
    if (p >= 70) {
      return 'text-emerald-300 border-emerald-500';
    }
    if (p >= 40) {
      return 'text-amber-300 border-amber-500';
    }
    return 'text-rose-300 border-rose-500';
  }

  get dimensionBadgeClass(): string {
    const p = this.dimensionPercent;
    if (p === null) {
      return 'text-slate-300';
    }
    if (p >= 70) {
      return 'text-emerald-300';
    }
    if (p >= 40) {
      return 'text-amber-300';
    }
    return 'text-rose-300';
  }
}


