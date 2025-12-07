import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { UserProgressService } from '../services/user-progress.service';
import { UserProgress } from '../models/user-progress.model';

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-profile.component.html',
  styleUrls: ['./candidate-profile.component.scss']
})
export class CandidateProfileComponent implements OnInit {

  loading = signal(true);
  error = signal<string | null>(null);
  email = signal<string | null>(null);
  progress = signal<UserProgress | null>(null);

  constructor(
    private route: ActivatedRoute,
    private userProgressService: UserProgressService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // 1) proovi URL-ist: /candidate-profile/:email
    const emailFromUrl = this.route.snapshot.paramMap.get('email');

    // 2) kui URL-is pole, proovi localStorage’ist
    const emailFromStorage =
      localStorage.getItem('userEmail') || localStorage.getItem('email');

    const email = emailFromUrl || emailFromStorage;

    if (!email) {
      this.error.set('Email puudub (URL-ist ega localStorage’ist ei leitud).');
      this.loading.set(false);
      return;
    }

    this.email.set(email);

    this.userProgressService.getProgress(email).subscribe({
      next: (p) => {
        this.progress.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Profiili laadimine ebaõnnestus.');
        this.loading.set(false);
      }
    });
  }

  goToWorkstyle(): void {
    this.router.navigate(['/workstyle-assessment']);
  }
}
