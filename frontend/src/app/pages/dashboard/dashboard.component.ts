import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AiService, DashboardStats } from '../../services/ai.service';
import { TrainingService, TrainingProgress } from '../../services/training.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  email = this.auth.getCurrentUserEmail();
  name = this.auth.getCurrentUserName();
  stats?: DashboardStats;
  progress?: TrainingProgress;
  loading = false;
  error = '';

  constructor(
    private ai: AiService,
    private training: TrainingService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.error = '';

    forkJoin({
      stats: this.ai.getDashboard(),
      progress: this.training.getProgress()
    }).subscribe({
      next: ({ stats, progress }) => {
        this.stats = stats;
        this.progress = progress;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load dashboard data.';
        this.loading = false;
      }
    });
  }

  formatDate(value?: string) {
    if (!value) return 'N/A';
    return new Date(value).toLocaleString();
  }
}

