import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AiService, DashboardStats } from '../../services/ai.service';
import { TrainingApiService, TrainingStatus } from '../../core/services/training-api.service';
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
  trainingStatus?: TrainingStatus;
  loading = false;
  error = '';

  constructor(
    private ai: AiService,
    private training: TrainingApiService,
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
      trainingStatus: this.training.getTrainingStatus()
    }).subscribe({
      next: ({ stats, trainingStatus }) => {
        this.stats = stats;
        this.trainingStatus = trainingStatus;
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

