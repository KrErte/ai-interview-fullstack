import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InterviewFitSnapshot } from '../../core/models/interview-session.model';

export interface InterviewFitInsight {
  type: 'STRENGTH' | 'RISK';
  text: string;
}

export interface InterviewFitDimensionBreakdown {
  key: string;
  label: string;
  scorePercent: number | null;
  band: string | null;
  insights: InterviewFitInsight[] | null;
}

export interface InterviewFitBreakdown {
  confidence: string | null; // LOW|MEDIUM|HIGH
  answeredCount: number;
  dimensions: InterviewFitDimensionBreakdown[] | null;
}

@Component({
  selector: 'app-fit-explain-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fit-explain-panel.component.html',
  styleUrls: ['./fit-explain-panel.component.scss']
})
export class FitExplainPanelComponent {
  @Input() open = false;
  @Input() fit: InterviewFitSnapshot | null = null;
  @Input() breakdown: InterviewFitBreakdown | null = null;

  @Output() closed = new EventEmitter<void>();

  onBackdropClick(): void {
    this.closed.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.stopPropagation();
      this.closed.emit();
    }
  }

  dimensionStrengths(): InterviewFitDimensionBreakdown[] {
    if (!this.breakdown?.dimensions) {
      return [];
    }
    return this.breakdown.dimensions.filter(d =>
      (d.insights || []).some(i => i.type === 'STRENGTH')
    );
  }

  dimensionRisks(): InterviewFitDimensionBreakdown[] {
    if (!this.breakdown?.dimensions) {
      return [];
    }
    return this.breakdown.dimensions.filter(d =>
      (d.insights || []).some(i => i.type === 'RISK')
    );
  }

  strengthInsights(dim: InterviewFitDimensionBreakdown): InterviewFitInsight[] {
    const list = dim.insights || [];
    return list.filter(i => i.type === 'STRENGTH');
  }

  riskInsights(dim: InterviewFitDimensionBreakdown): InterviewFitInsight[] {
    const list = dim.insights || [];
    return list.filter(i => i.type === 'RISK');
  }

  confidenceLabel(): string {
    const c = (this.breakdown?.confidence || '').toUpperCase();
    if (c === 'HIGH') return 'High confidence';
    if (c === 'MEDIUM') return 'Medium confidence';
    if (c === 'LOW') return 'Low confidence';
    return 'Unknown confidence';
  }

  confidenceClass(): string {
    const c = (this.breakdown?.confidence || '').toUpperCase();
    if (c === 'HIGH') return 'border-emerald-500 text-emerald-300';
    if (c === 'MEDIUM') return 'border-amber-500 text-amber-300';
    if (c === 'LOW') return 'border-slate-500 text-slate-200';
    return 'border-slate-700 text-slate-300';
  }

  bandLabel(band: string | null): string {
    const value = (band || '').toUpperCase();
    if (value === 'STRONG') return 'Strong';
    if (value === 'GOOD') return 'Good';
    if (value === 'NEEDS_WORK') return 'Needs work';
    return '—';
  }
}


