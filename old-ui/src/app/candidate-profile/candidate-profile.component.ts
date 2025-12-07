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
    if (!this.email) return;

    this.loading = true;
    this.error = null;

    this.userProgressService.getProgress(this.email).subscribe({
      next: (data) => {
        this.progress = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Viga andmete laadimisel';
        this.loading = false;
      }
    });
  }
}
