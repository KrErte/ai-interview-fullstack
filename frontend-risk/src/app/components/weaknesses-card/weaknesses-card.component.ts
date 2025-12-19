import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-weaknesses-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weaknesses-card.component.html'
})
export class WeaknessesCardComponent {
  @Input() weaknesses: string[] = [];
  @Input() coverage = 0;
  showAll = false;

  get displayedWeaknesses(): string[] {
    return this.showAll ? this.weaknesses : this.weaknesses.slice(0, 3);
  }
}
