import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-career-level-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './career-level-progress.component.html',
  styleUrls: ['./career-level-progress.component.scss'],
})
export class CareerLevelProgressComponent {
  /**
   * Praegune rollitase (nt "Junior", "Mid", "Senior", "Architect")
   */
  @Input() currentLevel: string = 'Junior';

  /**
   * Protsent järgmise tasemeni (0–100)
   */
  @Input() percentToNext: number = 0;

  levels: string[] = ['Junior', 'Mid', 'Senior', 'Architect'];

  isReached(level: string): boolean {
    const idx = this.levels.indexOf(level);
    const currentIdx = this.levels.indexOf(this.currentLevel);
    return idx <= currentIdx;
  }
}
