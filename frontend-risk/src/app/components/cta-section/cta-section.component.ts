import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cta-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cta-section.component.html'
})
export class CtaSectionComponent {
  @Input() roadmapAllowed = false;
  @Input() roadmapPrecision: 'LOW' | 'MEDIUM' | 'HIGH' | undefined;
  @Input() roadmap: string[] = [];
  @Input() loading = false;

  @Output() onGenerateRoadmap = new EventEmitter<void>();
}
