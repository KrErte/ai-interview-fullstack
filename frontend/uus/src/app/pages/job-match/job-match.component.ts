import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { JobService, JobMatchResult } from '../../services/job.service';

interface MatchCard {
  id: number;
  createdAt: Date;
  title?: string;
  location?: string;
  result: JobMatchResult;
}

@Component({
  selector: 'app-job-match',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-match.component.html'
})
export class JobMatchComponent {
  form = this.fb.group({
    targetRole: [''],
    location: [''],
    cvText: [this.jobService.getCachedCv()],
    jobDescription: ['', [Validators.required]],
    minScore: [0]
  });
  loading = false;
  error = '';
  matches: MatchCard[] = [];
  selectedMatch?: MatchCard;
  private counter = 1;

  constructor(private jobService: JobService, private fb: FormBuilder) {
    this.form.controls.cvText.valueChanges.subscribe(value => {
      this.jobService.setCachedCv(value || '');
    });
  }

  analyze() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Add a job description to run the analysis.';
      return;
    }
    this.error = '';
    this.loading = true;

    const payload = this.form.value;
    this.jobService.matchJobs({
      cvText: payload.cvText || '',
      jobDescription: payload.jobDescription || '',
      targetRole: payload.targetRole || ''
    }).subscribe({
      next: (results) => {
        const createdAt = new Date();
        const cards = (results || []).map((res) => ({
          id: this.counter++,
          createdAt,
          title: res.jobTitle || payload.targetRole || 'Target role',
          location: payload.location || 'Flexible',
          result: res
        }));
        this.matches = [...cards, ...this.matches];
        this.selectedMatch = cards[0];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Unable to run job match.';
      }
    });
  }

  filteredMatches() {
    return this.matches.filter(m => {
      const score = this.scoreValue(m.result);
      return score >= this.minScore;
    });
  }

  score(result: JobMatchResult) {
    return Math.round(this.scoreValue(result));
  }

  get minScore(): number {
    return this.form.controls.minScore.value || 0;
  }

  private scoreValue(result: JobMatchResult): number {
    const score = result.fitScore ?? result.matchScore ?? 0;
    return score > 1 ? score : score * 100;
  }
}

