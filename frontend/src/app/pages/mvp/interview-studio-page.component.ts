import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  InterviewSimulationRequest,
  InterviewSimulationResponse,
  MvpApiService,
  SeniorityLevel,
} from '../../core/services/mvp-api.service';

@Component({
  selector: 'app-interview-studio-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-studio-page.component.html',
})
export class InterviewStudioPageComponent {
  companyName = '';
  roleTitle = '';
  seniority: SeniorityLevel = 'Mid';

  loading = false;
  error = '';
  result?: InterviewSimulationResponse;

  readonly seniorityOptions: SeniorityLevel[] = ['Junior', 'Mid', 'Senior'];

  constructor(private mvpApi: MvpApiService) {}

  runInterview() {
    if (!this.companyName || !this.roleTitle) {
      this.error = 'Please provide both company name and role title.';
      return;
    }

    const payload: InterviewSimulationRequest = {
      companyName: this.companyName.trim(),
      roleTitle: this.roleTitle.trim(),
      seniority: this.seniority,
    };

    this.loading = true;
    this.error = '';
    this.result = undefined;

    this.mvpApi.simulateInterview(payload).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to run AI interview. Please try again in a moment.';
        this.loading = false;
      },
    });
  }

  saveSessionPlaceholder() {
    // Placeholder for future implementation
    // For now this is just a stub to show where save logic will live
    window.alert('Saving sessions is coming soon in a future release.');
  }
}


