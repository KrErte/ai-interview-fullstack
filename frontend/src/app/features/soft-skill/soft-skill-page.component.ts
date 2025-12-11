import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SoftSkillEvaluationFormComponent } from './soft-skill-evaluation-form.component';
import { SoftSkillMergedProfileComponent } from './soft-skill-merged-profile.component';

type SoftSkillTab = 'evaluation' | 'merged';

@Component({
  selector: 'app-soft-skill-page',
  standalone: true,
  imports: [
    CommonModule,
    SoftSkillEvaluationFormComponent,
    SoftSkillMergedProfileComponent
  ],
  templateUrl: './soft-skill-page.component.html',
  styleUrls: ['./soft-skill-page.component.scss']
})
export class SoftSkillPageComponent {
  activeTab: SoftSkillTab = 'evaluation';

  setActiveTab(tab: SoftSkillTab): void {
    this.activeTab = tab;
  }

  isActive(tab: SoftSkillTab): boolean {
    return this.activeTab === tab;
  }
}


