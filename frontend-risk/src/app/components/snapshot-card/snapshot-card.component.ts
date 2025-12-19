import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-snapshot-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './snapshot-card.component.html'
})
export class SnapshotCardComponent {
  @Input() riskPercent = 0;
  @Input() confidence = 0;
  @Input() signalsCount = 0;
  @Input() coverage = 0;

  formatConfidence(): string {
    return `${(this.confidence * 100).toFixed(1)}%`;
  }

  getRiskColorClass(): string {
    if (this.riskPercent >= 70) return 'text-emerald-400';
    if (this.riskPercent >= 40) return 'text-amber-400';
    return 'text-rose-400';
  }

  getRiskLabel(): string {
    if (this.riskPercent >= 70) return 'Low Risk - Well Prepared';
    if (this.riskPercent >= 40) return 'Medium Risk - Room to Improve';
    return 'High Risk - Needs Attention';
  }
}
