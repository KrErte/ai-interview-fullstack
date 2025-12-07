import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AiService, UserProfile } from '../../services/ai.service';
import { TrainingService, TrainingProgress } from '../../services/training.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  profile?: UserProfile;
  progress?: TrainingProgress;
  form = this.fb.group({
    fullName: [this.auth.getCurrentUserName() || '', [Validators.required]],
    currentRole: [''],
    targetRole: [''],
    yearsOfExperience: [null as number | null],
    skills: [''],
    bio: ['']
  });
  saving = false;
  loading = false;
  message = '';
  error = '';

  constructor(
    private ai: AiService,
    private training: TrainingService,
    private auth: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    this.ai.getUserProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.form.patchValue({
          fullName: profile.fullName || '',
          currentRole: profile.currentRole || '',
          targetRole: profile.targetRole || '',
          yearsOfExperience: profile.yearsOfExperience ?? null,
          skills: profile.skills || '',
          bio: profile.bio || ''
        });
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Failed to load profile.';
      }
    });

    this.training.getProgress().subscribe({
      next: (progress) => (this.progress = progress),
      error: () => {}
    });
  }

  saveProfile() {
    this.saving = true;
    this.error = '';
    this.message = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.saving = false;
      return;
    }

    const payload = this.form.value as UserProfile;
    this.ai.saveUserProfile(payload).subscribe({
      next: (saved) => {
        this.auth.setDisplayName(saved.fullName || '');
        this.profile = saved;
        this.saving = false;
        this.message = 'Profile saved.';
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save profile.';
      }
    });
  }
}

