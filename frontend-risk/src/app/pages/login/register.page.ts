import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="mx-auto flex min-h-[80vh] max-w-md flex-col justify-center py-6">
      <a routerLink="/" class="rise flex items-center gap-2 text-[19px] font-extrabold tracking-tight" style="--i:0">
        <span class="h-2.5 w-2.5 rounded-full bg-accent"></span>
        Futureproof
      </a>
      <h1 class="display rise mt-10 text-[clamp(2.5rem,8vw,3.5rem)]" style="--i:1">Create an <span class="text-accent">account.</span></h1>
      <p class="rise mt-4 text-[16px] leading-relaxed text-muted" style="--i:2">Keep your results and come back to your learning path.</p>

      <form [formGroup]="form" (ngSubmit)="register()" class="card rise mt-8 space-y-5 p-6 sm:p-8" style="--i:3" novalidate>
        <div>
          <label for="fullName" class="mb-2 block text-[14px] font-medium text-muted">Full name</label>
          <input id="fullName" type="text" formControlName="fullName" autocomplete="name" class="field" placeholder="Jane Doe" />
          <p *ngIf="form.controls.fullName.touched && form.controls.fullName.invalid" class="mt-2 text-[13px] text-band-very-high">Full name is required</p>
        </div>
        <div>
          <label for="email" class="mb-2 block text-[14px] font-medium text-muted">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" class="field" placeholder="you@example.com" />
          <p *ngIf="form.controls.email.touched && form.controls.email.invalid" class="mt-2 text-[13px] text-band-very-high">Please enter a valid email</p>
        </div>
        <div>
          <label for="password" class="mb-2 block text-[14px] font-medium text-muted">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="new-password" class="field" placeholder="At least 6 characters" />
          <p *ngIf="form.controls.password.touched && form.controls.password.invalid" class="mt-2 text-[13px] text-band-very-high">Password must be at least 6 characters</p>
        </div>

        <p *ngIf="error" class="rounded-xl border border-band-very-high px-4 py-3 text-[14px] text-band-very-high" role="alert">{{ error }}</p>

        <button type="submit" [disabled]="loading" class="btn-accent w-full">
          <span *ngIf="!loading">Create account</span>
          <span *ngIf="loading">Creating account…</span>
          <span *ngIf="!loading" class="arrow" aria-hidden="true">&rarr;</span>
        </button>
      </form>

      <p class="rise mt-6 text-center text-[14px] text-muted" style="--i:4">
        Already have an account?
        <a routerLink="/login" class="ml-1 font-semibold text-accent hover:underline">Sign in</a>
      </p>
    </div>
  `
})
export class RegisterPageComponent {
  form = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  loading = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  register(): void {
    if (this.form.invalid) {
      this.error = '';
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    this.auth.register({
      fullName: this.form.value.fullName || '',
      email: this.form.value.email || '',
      password: this.form.value.password || ''
    }).subscribe({
      next: () => {
        this.router.navigate(['/ai-test']);
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 409) {
          this.error = 'An account with this email already exists. Sign in instead.';
        } else if (err?.status === 400) {
          this.error = 'Please enter a valid email and a password of at least 6 characters.';
        } else {
          this.error = err?.error?.message || 'Registration failed. Please try again.';
        }
      }
    });
  }
}
