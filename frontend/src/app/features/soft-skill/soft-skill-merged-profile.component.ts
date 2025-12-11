import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  SoftSkillApiService,
  SoftSkillDimension,
  SoftSkillMergedProfile,
  SoftSkillConfidence
} from './soft-skill-api.service';

@Component({
  selector: 'app-soft-skill-merged-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './soft-skill-merged-profile.component.html',
  styleUrls: ['./soft-skill-merged-profile.component.scss']
})
export class SoftSkillMergedProfileComponent implements OnInit {
  form = this.fb.group({
    email: ['candidate@example.com', [Validators.required, Validators.email]]
  });

  loading = false;
  errorMessage = '';
  mergedProfile: SoftSkillMergedProfile | null = null;
  hasSubmitted = false;
  dimensions: SoftSkillDimension[] = [];
  dimensionsLoading = false;
  dimensionsError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: SoftSkillApiService
  ) {}

  ngOnInit(): void {
    this.loadDimensions();
  }

  get hasDimensions(): boolean {
    return !!this.mergedProfile?.dimensions?.length;
  }

  labelForDimension(key: string): string {
    return (
      this.dimensions.find(d => d.key === key)?.displayName ||
      key
    );
  }

  confidenceBadgeClass(confidence: SoftSkillConfidence): string {
    switch (confidence) {
      case 'high':
        return 'bg-emerald-500/15 text-emerald-200 border border-emerald-500/40';
      case 'medium':
        return 'bg-amber-500/15 text-amber-200 border border-amber-500/40';
      default:
        return 'bg-rose-500/15 text-rose-200 border border-rose-500/40';
    }
  }

  loadProfile(): void {
    this.errorMessage = '';
    this.hasSubmitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Please enter a valid email.';
      return;
    }

    const { email } = this.form.getRawValue();
    const trimmedEmail = (email || '').trim();

    if (!trimmedEmail) {
      this.errorMessage = 'Please enter a valid email.';
      return;
    }

    this.loading = true;
    this.mergedProfile = null;

    this.api
      .getMergedProfile(trimmedEmail)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: profile => {
          this.mergedProfile = profile;
        },
        error: err => {
          this.errorMessage =
            err?.error?.message ||
            'Could not load merged profile. Please check email and try again.';
        }
      });
  }

  private loadDimensions(): void {
    this.dimensionsLoading = true;
    this.dimensionsError = '';

    this.api
      .getDimensions()
      .pipe(finalize(() => (this.dimensionsLoading = false)))
      .subscribe({
        next: dims => {
          this.dimensions = dims || [];
        },
        error: err => {
          this.dimensionsError =
            err?.error?.message ||
            'Could not load dimensions for labels.';
        }
      });
  }
}

