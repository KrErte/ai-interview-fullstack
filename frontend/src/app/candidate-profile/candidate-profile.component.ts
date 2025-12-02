import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserProgressService } from '../services/user-progress.service';
import { UserProgress } from '../models/user-progress.model';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-profile.component.html',
  styleUrls: ['./candidate-profile.component.scss']
})
export class CandidateProfileComponent implements OnInit {

  email: string | null = null;
  progress: UserProgress | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private userProgressService: UserProgressService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.email = this.authService.getCurrentUserEmail();
    if (this.email) {
      this.loadProgress();
    }
  }

  loadProgress(): void {
    if (!this.email) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.userProgressService.getProgress(this.email).subscribe({
      next: (data) => {
        this.progress = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load user progress', err);
        this.error = 'Edenemise laadimine ebaõnnestus. Proovi hiljem uuesti.';
        this.loading = false;
      }
    });
  }

  // --- UI helperid ---

  get lastActiveText(): string {
    if (!this.progress?.lastActive) {
      return '-';
    }
    try {
      const d = new Date(this.progress.lastActive);
      return d.toLocaleString('et-EE', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '-';
    }
  }

  get lastMatchScoreText(): string {
    if (this.progress?.lastMatchScore == null) {
      return '-';
    }
    return `${this.progress.lastMatchScore}%`;
  }

  get lastMatchSummaryText(): string {
    return this.progress?.lastMatchSummary || '-';
  }

  get totalJobAnalyses(): number {
    return this.progress?.totalJobAnalyses ?? 0;
  }

  get totalTrainingSessions(): number {
    return this.progress?.totalTrainingSessions ?? 0;
  }

  get trainingProgressPercent(): number {
    return this.progress?.trainingProgressPercent ?? 0;
  }
}
