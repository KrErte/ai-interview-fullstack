import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { JobService, JobMatchResult } from '../../services/job.service';

@Component({
  selector: 'app-job-analysis',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-analysis.component.html'
})
export class JobAnalysisComponent {
  form = this.fb.group({
    cvText: [this.jobService.getCachedCv()],
    jobDescription: ['', [Validators.required]]
  });
  loading = false;
  error = '';
  result?: JobMatchResult;

  constructor(private jobService: JobService, private fb: FormBuilder) {
    this.form.controls.cvText.valueChanges.subscribe(value => this.jobService.setCachedCv(value || ''));
  }

  analyze() {
    if (this.form.invalid) {
        this.form.markAllAsTouched();
      this.error = 'Please paste a job description.';
      return;
    }
    this.loading = true;
    this.error = '';

    const payload = this.form.value;

    this.jobService.analyzeJob({
      cvText: payload.cvText || '',
      jobDescription: payload.jobDescription || ''
    }).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Analysis failed.';
      }
    });
  }

  score(): number {
    if (!this.result) return 0;
    const value = (this.result.fitScore ?? this.result.matchScore) || 0;
    return Math.round(value > 1 ? value : value * 100);
  }
}

