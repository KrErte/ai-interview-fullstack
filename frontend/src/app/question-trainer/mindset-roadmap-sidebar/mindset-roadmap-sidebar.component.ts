import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

interface RoadmapItem {
  id: string;
  title: string;
  description: string;
  level: 'junior' | 'mid' | 'senior' | 'architect';
  skillTag: string;
  completed: boolean;
}

@Component({
  selector: 'app-mindset-roadmap-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mindset-roadmap-sidebar.component.html',
  styleUrls: ['./mindset-roadmap-sidebar.component.scss']
})
export class MindsetRoadmapSidebarComponent {

  @Input() items: RoadmapItem[] = [];
  @Input() activeItemId: string | null = null;

  @Output() selectItem = new EventEmitter<string>();

  onClickItem(id: string): void {
    this.selectItem.emit(id);
  }

  getLevelLabel(level: RoadmapItem['level']): string {
    switch (level) {
      case 'junior':
        return 'Junior';
      case 'mid':
        return 'Mid';
      case 'senior':
        return 'Senior';
      case 'architect':
        return 'Architect';
      default:
        return level;
    }
  }
}
