import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SkillMatrixApiService } from '../../services/skill-matrix-api.service';
import { StartInterviewRequest } from '../../models/interview.model';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-skill-matrix-start-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-matrix-start-page.component.html',
  styleUrls: ['./skill-matrix-start-page.component.scss'],
})
export class SkillMatrixStartPageComponent {
  jobRole = '';
  candidateEmail = '';

  loading = false;
  error = '';

  constructor(
    private api: SkillMatrixApiService,
    private router: Router,
    private auth: AuthService
  ) {
    this.candidateEmail =
      this.auth.getCurrentUserEmail() ||
      localStorage.getItem('aiim_email') ||
      '';
  }

  startInterview() {
    if (!this.jobRole || !this.candidateEmail) {
      this.error = 'Please provide both job/role and candidate email.';
      return;
    }

    const payload: StartInterviewRequest = {
      jobRole: this.jobRole.trim(),
      candidateEmail: this.candidateEmail.trim(),
    };

    this.loading = true;
    this.error = '';

    this.api.startInterview(payload).subscribe({
      next: res => {
        this.loading = false;
        const sessionId = res.sessionId || res.session.id;
        this.router.navigate(['/skill-matrix/session', sessionId]);
      },
      error: err => {
        this.loading = false;
        this.error =
          err?.error?.message ||
          'Failed to start interview. Please try again in a moment.';
      },
    });
  }
}


