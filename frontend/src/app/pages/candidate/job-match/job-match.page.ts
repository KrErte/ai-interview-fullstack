import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { JobMatchRequest, JobMatchResult, JobMatchService } from '../../../services/job-match.service';
import { JobMatchStateService } from '../../../services/job-match-state.service';

interface JobMatchViewModel extends JobMatchResult {
  matchPercent: number | null;
}

@Component({
  selector: 'app-job-match-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-match.page.html',
  styleUrls: ['./job-match.page.scss']
})
export class JobMatchPageComponent {
  form = this.fb.group({
    jobDescription: ['', [Validators.required, Validators.minLength(20)]],
  });

  cvFileName: string | null = null;
  cvText: string | null = null;
  uploadError: string | null = null;

  uploadingCv = false;
  analyzing = false;
  analyzeError: string | null = null;

  result: JobMatchViewModel | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly jobMatchService: JobMatchService,
    private readonly authService: AuthService,
    private readonly jobMatchState: JobMatchStateService
  ) {}

  get analyzeDisabled(): boolean {
    const description = this.form.value.jobDescription?.trim();
    return this.uploadingCv || this.analyzing || !this.cvText || !description;
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.uploadError = null;
    this.analyzeError = null;
    this.result = null;

    if (!file) {
      this.cvFileName = null;
      this.cvText = null;
      return;
    }

    if (file.type !== 'application/pdf') {
      this.uploadError = 'Please select a PDF file.';
      this.cvFileName = null;
      this.cvText = null;
      return;
    }

    this.cvFileName = file.name;
    this.uploadingCv = true;

    this.jobMatchService.uploadCv(file).subscribe({
      next: (res) => {
        this.cvText = res?.text?.trim() ? res.text : null;
        if (!this.cvText) {
          this.uploadError = 'Could not read text from the PDF. Try another file.';
        }
      },
      error: (err) => {
        this.uploadError = err?.error?.message || 'CV upload failed. Please try again.';
        this.cvText = null;
      },
      complete: () => {
        this.uploadingCv = false;
      }
    });
  }

  analyze(): void {
    if (this.analyzeDisabled) {
      this.analyzeError = 'Upload your CV and add the job description first.';
      return;
    }

    const jobDescription = this.form.value.jobDescription?.trim() ?? '';

    const payload: JobMatchRequest = {
      cvText: this.cvText ?? '',
      jobDescription,
      email: this.authService.getCurrentUserEmail() ?? undefined,
    };

    this.analyzing = true;
    this.result = null;
    this.analyzeError = null;

    this.jobMatchService.analyzeMatch(payload).subscribe({
      next: (res) => {
        const normalizedResult: JobMatchResult = {
          ...res,
          matchScore: res.matchScore ?? null,
          strengths: res.strengths ?? [],
          weaknesses: res.weaknesses ?? [],
          missingSkills: res.missingSkills ?? [],
          roadmap: res.roadmap ?? [],
          suggestedImprovements: res.suggestedImprovements ?? '',
          summary: res.summary ?? '',
          trainingSessionId: res.trainingSessionId,
        };

        this.result = {
          ...normalizedResult,
          ...res,
          matchPercent: this.toPercent(res.matchScore),
        };
        this.jobMatchState.setLastResult(normalizedResult);
        this.jobMatchState.setCurrentTrainingSessionId(res.trainingSessionId ?? null);
        this.analyzing = false;
      },
      error: (err) => {
        this.analyzeError = err?.error?.message || 'Analysis failed. Please try again.';
        this.analyzing = false;
      }
    });
  }

  private toPercent(score?: number | null): number | null {
    if (score == null) {
      return null;
    }
    return score > 1 ? Math.round(score) : Math.round(score * 100);
  }
}

