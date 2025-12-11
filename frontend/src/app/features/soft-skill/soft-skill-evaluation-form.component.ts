import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { finalize } from 'rxjs';
import {
  SoftSkillApiService,
  SoftSkillDimension,
  SoftSkillEvaluationRequest,
  SoftSkillScoreRequest,
  SoftSkillSourceType
} from './soft-skill-api.service';

@Component({
  selector: 'app-soft-skill-evaluation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './soft-skill-evaluation-form.component.html',
  styleUrls: ['./soft-skill-evaluation-form.component.scss']
})
export class SoftSkillEvaluationFormComponent implements OnInit {
  form: FormGroup;

  dimensions: SoftSkillDimension[] = [];
  loadingDimensions = false;
  dimensionsError = '';

  submitting = false;
  submitError = '';
  submitSuccess = '';

  readonly sourceTypes: SoftSkillSourceType[] = [
    'HR',
    'TECH',
    'TEAM_LEAD',
    'AI_INTERVIEW',
    'CV_ANALYSIS',
    'SELF_ASSESSMENT',
    'OTHER'
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: SoftSkillApiService
  ) {
    this.form = this.fb.group({
      email: [
        'candidate@example.com',
        [Validators.required, Validators.email]
      ],
      jobContext: [''],
      sourceType: ['HR', Validators.required],
      sourceLabel: [''],
      overallComment: [''],
      scores: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.loadDimensions();
  }

  get scoresArray(): FormArray {
    return this.form.get('scores') as FormArray;
  }

  get canSubmit(): boolean {
    return (
      this.scoresArray.length > 0 &&
      !this.submitting &&
      !this.loadingDimensions &&
      this.form.valid
    );
  }

  addScore(): void {
    this.scoresArray.push(this.createScoreGroup());
  }

  removeScore(index: number): void {
    if (this.scoresArray.length <= 1) {
      return;
    }
    this.scoresArray.removeAt(index);
  }

  onSubmit(): void {
    this.submitError = '';
    this.submitSuccess = '';

    if (!this.dimensions.length) {
      this.submitError = 'Please load dimensions before submitting.';
      return;
    }

    if (this.form.invalid || this.scoresArray.length === 0) {
      this.form.markAllAsTouched();
      this.submitError = 'Please fill in the required fields and add at least one score.';
      return;
    }

    const raw = this.form.getRawValue();
    const scores: SoftSkillScoreRequest[] = raw.scores.map(
      (score: any) =>
        ({
          dimensionKey: score.dimensionKey,
          score: Number(score.score),
          explanation: score.explanation?.trim()
            ? score.explanation.trim()
            : undefined
        }) as SoftSkillScoreRequest
    );

    const payload: SoftSkillEvaluationRequest = {
      email: raw.email.trim(),
      jobContext: raw.jobContext?.trim() || undefined,
      sourceType: raw.sourceType,
      sourceLabel: raw.sourceLabel?.trim() || undefined,
      overallComment: raw.overallComment?.trim() || undefined,
      scores
    };

    this.submitting = true;

    this.api
      .createEvaluation(payload)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.submitSuccess = 'Soft skill evaluation saved.';
          this.resetScoresAfterSubmit();
        },
        error: err => {
          this.submitError =
            err?.error?.message ||
            'Could not save the evaluation. Please try again.';
        }
      });
  }

  trackByIndex(index: number): number {
    return index;
  }

  private loadDimensions(): void {
    this.loadingDimensions = true;
    this.dimensionsError = '';

    this.api
      .getDimensions()
      .pipe(finalize(() => (this.loadingDimensions = false)))
      .subscribe({
        next: dimensions => {
          this.dimensions = dimensions || [];
          this.resetScoresAfterDimensions();
        },
        error: err => {
          this.dimensionsError =
            err?.error?.message ||
            'Could not load dimensions. Please try again.';
        }
      });
  }

  private resetScoresAfterDimensions(): void {
    this.scoresArray.clear();
    if (this.dimensions.length) {
      this.addScore();
    }
  }

  private resetScoresAfterSubmit(): void {
    this.form.get('overallComment')?.reset('');
    this.form.get('sourceLabel')?.reset('');
    this.form.get('sourceType')?.setValue('HR');
    this.resetScoresAfterDimensions();
  }

  private createScoreGroup(): FormGroup {
    const defaultDimensionKey = this.dimensions[0]?.key || '';

    return this.fb.group({
      dimensionKey: [defaultDimensionKey, Validators.required],
      score: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
      explanation: ['']
    });
  }
}


