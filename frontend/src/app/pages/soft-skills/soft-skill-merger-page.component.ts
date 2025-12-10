import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  SoftSkillService,
  SoftSkillDimension,
  SoftSkillEvaluationResponse,
  SoftSkillMergedProfileResponse,
  SoftSkillSource
} from '../../services/soft-skill.service';
import { AuthService } from '../../services/auth.service';

interface NewEvaluationForm {
  source: SoftSkillSource | '';
  dimension: SoftSkillDimension | '';
  score: number | null;
  comment: string;
}

@Component({
  selector: 'app-soft-skill-merger-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './soft-skill-merger-page.component.html',
  styleUrls: ['./soft-skill-merger-page.component.scss']
})
export class SoftSkillMergerPageComponent {
  email = this.auth.getCurrentUserEmail() || '';

  evaluations: SoftSkillEvaluationResponse[] = [];
  mergedProfile: SoftSkillMergedProfileResponse | null = null;

  loadingEvaluations = false;
  loadingProfile = false;
  savingEvaluation = false;
  mergingProfile = false;

  showAddForm = false;

  error = '';
  evaluationError = '';
  mergeError = '';
  successMessage = '';

  readonly sources: { value: SoftSkillSource; label: string }[] = [
    { value: 'HR', label: 'HR' },
    { value: 'TECH_LEAD', label: 'Tech Lead' },
    { value: 'TEAM_LEAD', label: 'Team Lead' }
  ];

  readonly dimensions: { value: SoftSkillDimension; label: string }[] = [
    { value: 'ADAPTABILITY', label: 'Adaptability' },
    { value: 'GROWTH_MINDSET', label: 'Growth mindset' },
    { value: 'COMMUNICATION', label: 'Communication' },
    { value: 'COLLABORATION', label: 'Collaboration' },
    { value: 'OWNERSHIP', label: 'Ownership' },
    { value: 'PROBLEM_SOLVING', label: 'Problem solving' },
    { value: 'LEADERSHIP', label: 'Leadership' }
  ];

  newEvaluation: NewEvaluationForm = {
    source: 'HR',
    dimension: 'ADAPTABILITY',
    score: 70,
    comment: ''
  };

  constructor(
    private readonly softSkills: SoftSkillService,
    private readonly auth: AuthService
  ) {}

  useMyEmail(): void {
    const current = this.auth.getCurrentUserEmail();
    if (current) {
      this.email = current;
    }
  }

  loadData(): void {
    if (!this.email) {
      this.error = 'Please enter an email address.';
      return;
    }

    this.error = '';
    this.successMessage = '';
    this.loadEvaluations();
    this.loadMergedProfile();
  }

  private loadEvaluations(): void {
    this.loadingEvaluations = true;
    this.evaluationError = '';

    this.softSkills.getEvaluations(this.email).subscribe({
      next: (items) => {
        this.evaluations = items || [];
        this.loadingEvaluations = false;
      },
      error: (err) => {
        this.loadingEvaluations = false;
        this.evaluationError =
          err?.error?.message || 'Failed to load soft skill evaluations.';
      }
    });
  }

  private loadMergedProfile(): void {
    this.loadingProfile = true;
    this.mergeError = '';

    this.softSkills.getMergedProfile(this.email).subscribe({
      next: (profile) => {
        this.mergedProfile = profile;
        this.loadingProfile = false;
      },
      error: () => {
        // Having no profile yet is not an error – just show empty state.
        this.mergedProfile = null;
        this.loadingProfile = false;
      }
    });
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    this.evaluationError = '';
    this.successMessage = '';
  }

  saveEvaluation(): void {
    if (!this.email) {
      this.evaluationError = 'Please enter an email before adding evaluations.';
      return;
    }

    if (!this.newEvaluation.source || !this.newEvaluation.dimension) {
      this.evaluationError = 'Please select both source and dimension.';
      return;
    }

    if (
      this.newEvaluation.score === null ||
      this.newEvaluation.score < 0 ||
      this.newEvaluation.score > 100
    ) {
      this.evaluationError = 'Score must be between 0 and 100.';
      return;
    }

    this.savingEvaluation = true;
    this.evaluationError = '';

    const payload = {
      email: this.email,
      source: this.newEvaluation.source as SoftSkillSource,
      dimension: this.newEvaluation.dimension as SoftSkillDimension,
      score: this.newEvaluation.score,
      comment: this.newEvaluation.comment || ''
    };

    this.softSkills.createEvaluation(payload).subscribe({
      next: () => {
        this.savingEvaluation = false;
        this.successMessage = 'Evaluation saved.';
        this.newEvaluation = {
          source: 'HR',
          dimension: 'ADAPTABILITY',
          score: 70,
          comment: ''
        };
        this.loadEvaluations();
      },
      error: (err) => {
        this.savingEvaluation = false;
        this.evaluationError =
          err?.error?.message || 'Failed to save evaluation.';
      }
    });
  }

  runMerge(): void {
    if (!this.email) {
      this.mergeError = 'Please enter an email before running the merge.';
      return;
    }

    this.mergingProfile = true;
    this.mergeError = '';
    this.successMessage = '';

    this.softSkills.mergeProfile(this.email).subscribe({
      next: () => {
        this.mergingProfile = false;
        this.successMessage = 'AI merge completed.';
        this.loadMergedProfile();
      },
      error: (err) => {
        this.mergingProfile = false;
        this.mergeError =
          err?.error?.message || 'Failed to run AI merge for this profile.';
      }
    });
  }

  trackByEvaluationId(_index: number, item: SoftSkillEvaluationResponse): string {
    return item.id || `${item.email}-${item.dimension}-${item.source}-${item.createdAt}`;
  }
}


