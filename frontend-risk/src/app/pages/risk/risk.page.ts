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
  template: `
    <div class="min-h-screen">
      <!-- Page Header -->
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-slate-100 mb-4">
          <span class="gradient-text">Career Risk</span> Assessment
        </h1>
        <p class="text-lg text-slate-400 max-w-2xl mx-auto">
          Understand your career readiness and get personalized recommendations to minimize risk.
        </p>
      </div>

      <!-- Assessment Input (if no assessment yet) -->
      <div *ngIf="!assessment && !loading" class="max-w-2xl mx-auto mb-12">
        <div class="rounded-3xl border border-slate-800/50 bg-gradient-to-br from-slate-900/80 to-slate-900/40 p-8 backdrop-blur-sm">
          <div class="flex items-center gap-3 mb-6">
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500/10">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <div>
              <h2 class="text-xl font-semibold text-slate-100">Start Your Assessment</h2>
              <p class="text-sm text-slate-500">Paste your CV and target job description</p>
            </div>
          </div>

          <form [formGroup]="form" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-2">Your CV / Resume</label>
              <textarea
                formControlName="cvText"
                rows="5"
                class="w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none transition-all resize-none"
                placeholder="Paste your CV text here..."
              ></textarea>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-300 mb-2">Job Description</label>
              <textarea
                formControlName="jobDescription"
                rows="5"
                class="w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none transition-all resize-none"
                placeholder="Paste the job description you're targeting..."
              ></textarea>
              <p *ngIf="form.controls.jobDescription.touched && form.controls.jobDescription.invalid" class="text-rose-400 text-sm mt-2">
                Job description is required
              </p>
            </div>

            <button
              type="button"
              (click)="runAssessment()"
              [disabled]="form.invalid || loading"
              class="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-4 font-semibold text-white shadow-lg shadow-emerald-500/25 transition-all hover:shadow-emerald-500/40 hover:scale-[1.01] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            >
              <span class="flex items-center justify-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                </svg>
                Analyze My Career Risk
              </span>
            </button>
          </form>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="loading" class="flex flex-col items-center justify-center py-20">
        <div class="relative">
          <div class="h-20 w-20 rounded-full border-4 border-slate-800"></div>
          <div class="absolute top-0 left-0 h-20 w-20 rounded-full border-4 border-emerald-500 border-t-transparent animate-spin"></div>
        </div>
        <p class="text-lg text-slate-300 mt-6">Analyzing your career profile...</p>
        <p class="text-sm text-slate-500 mt-2">This may take a moment</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error" class="max-w-2xl mx-auto mb-8">
        <div class="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6 flex items-start gap-4">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-500/20 shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-rose-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p class="text-rose-300 font-medium">Assessment Failed</p>
            <p class="text-sm text-rose-200/70 mt-1">{{ error }}</p>
            <button
              (click)="resetAssessment()"
              class="text-sm text-rose-400 hover:text-rose-300 mt-3 underline"
            >
              Try again
            </button>
          </div>
        </div>
      </div>

      <!-- Assessment Results -->
      <div *ngIf="assessment && !loading" class="space-y-8">
        <!-- Summary Banner -->
        <div *ngIf="assessment.summary" class="rounded-2xl border border-slate-800/50 bg-slate-900/40 p-6">
          <p class="text-slate-300 text-lg leading-relaxed text-center">
            {{ assessment.summary }}
          </p>
        </div>

        <!-- Cards Grid -->
        <div class="grid gap-8 lg:grid-cols-2">
          <!-- Snapshot Card (full width on mobile, left on desktop) -->
          <app-snapshot-card
            [riskPercent]="assessment.riskPercent"
            [confidence]="assessment.confidence"
            [signalsCount]="assessment.signalsCount"
            [coverage]="assessment.coverage"
            class="lg:col-span-2"
          ></app-snapshot-card>

          <!-- Strengths Card -->
          <app-strengths-card
            [strengths]="assessment.strengths"
          ></app-strengths-card>

          <!-- Weaknesses Card -->
          <app-weaknesses-card
            [weaknesses]="assessment.weaknesses"
            [coverage]="assessment.coverage"
          ></app-weaknesses-card>
        </div>

        <!-- CTA Section -->
        <app-cta-section
          [roadmapAllowed]="assessment.roadmapAllowedByDepth"
          [roadmapPrecision]="assessment.roadmapPrecision"
          [roadmap]="assessment.roadmap || []"
          [loading]="generatingRoadmap"
          (onGenerateRoadmap)="generateRoadmap()"
        ></app-cta-section>

        <!-- Reset Button -->
        <div class="text-center pt-4">
          <button
            (click)="resetAssessment()"
            class="text-slate-400 hover:text-slate-300 text-sm transition-colors"
          >
            Start a new assessment
          </button>
        </div>
      </div>
    </div>
  `
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
