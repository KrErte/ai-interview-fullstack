import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { SnapshotCardComponent } from '../../components/snapshot-card/snapshot-card.component';
import { StrengthsCardComponent } from '../../components/strengths-card/strengths-card.component';
import { WeaknessesCardComponent } from '../../components/weaknesses-card/weaknesses-card.component';
import { CtaSectionComponent } from '../../components/cta-section/cta-section.component';
import { RiskService } from '../../services/risk.service';
import { RiskStateService } from '../../services/risk-state.service';
import { RiskAssessment } from '../../models/risk.model';

@Component({
  selector: 'app-risk-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SnapshotCardComponent,
    StrengthsCardComponent,
    WeaknessesCardComponent,
    CtaSectionComponent
  ],
  templateUrl: './risk.page.html'
})
export class RiskPageComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form = this.fb.group({
    cvText: [this.riskService.getCachedCv()],
    jobDescription: ['', [Validators.required]]
  });

  loading = false;
  generatingRoadmap = false;
  error: string | null = null;
  assessment: RiskAssessment | null = null;

  constructor(
    private fb: FormBuilder,
    private riskService: RiskService,
    private riskState: RiskStateService
  ) {}

  ngOnInit(): void {
    // Subscribe to state changes
    this.riskState.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.loading = state.loading;
        this.error = state.error;
        this.assessment = state.assessment;
      });

    // Cache CV text on change
    this.form.controls.cvText.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => this.riskService.setCachedCv(value || ''));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  runAssessment(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.riskState.setLoading(true);

    const payload = {
      cvText: this.form.value.cvText || '',
      jobDescription: this.form.value.jobDescription || ''
    };

    this.riskService.analyzeJob(payload).subscribe({
      next: (assessment) => {
        this.riskState.setAssessment(assessment);
      },
      error: (err) => {
        this.riskState.setError(err?.error?.message || 'Failed to analyze. Please try again.');
      }
    });
  }

  generateRoadmap(): void {
    if (!this.assessment) return;

    this.generatingRoadmap = true;

    const payload = {
      cvText: this.form.value.cvText || '',
      jobDescription: this.form.value.jobDescription || ''
    };

    this.riskService.generateRoadmap(payload).subscribe({
      next: (roadmap) => {
        this.generatingRoadmap = false;
        if (this.assessment) {
          this.riskState.setAssessment({
            ...this.assessment,
            roadmap
          });
        }
      },
      error: () => {
        this.generatingRoadmap = false;
      }
    });
  }

  resetAssessment(): void {
    this.riskState.reset();
  }
}
