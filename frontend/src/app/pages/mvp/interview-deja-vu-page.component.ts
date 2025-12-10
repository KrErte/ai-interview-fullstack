import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  DejaVuPredictRequest,
  DejaVuPredictResponse,
  MvpApiService,
} from '../../core/services/mvp-api.service';

@Component({
  selector: 'app-interview-deja-vu-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-deja-vu-page.component.html',
})
export class InterviewDejaVuPageComponent {
  jobDescription = '';
  loading = false;
  error = '';
  result?: DejaVuPredictResponse;

  constructor(private mvpApi: MvpApiService) {}

  predict() {
    if (!this.jobDescription.trim()) {
      this.error = 'Paste a job description first.';
      return;
    }

    const payload: DejaVuPredictRequest = {
      jobDescription: this.jobDescription.trim(),
    };

    this.loading = true;
    this.error = '';
    this.result = undefined;

    this.mvpApi.predictDejaVuQuestions(payload).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to predict questions. Please try again in a moment.';
        this.loading = false;
      },
    });
  }
}


