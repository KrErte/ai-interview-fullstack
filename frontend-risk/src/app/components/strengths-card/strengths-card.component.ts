import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-strengths-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './strengths-card.component.html'
})
export class StrengthsCardComponent {
  @Input() strengths: string[] = [];
  showAll = false;

  get displayedStrengths(): string[] {
    return this.showAll ? this.strengths : this.strengths.slice(0, 3);
  }
}
