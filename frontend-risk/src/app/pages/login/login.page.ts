import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="mx-auto flex min-h-[80vh] max-w-md flex-col justify-center py-6">
      <a routerLink="/" class="rise flex items-center gap-2 text-[19px] font-extrabold tracking-tight" style="--i:0">
        <span class="h-2.5 w-2.5 rounded-full bg-accent"></span>
        Futureproof
      </a>
      <h1 class="display rise mt-10 text-[clamp(2.5rem,8vw,3.5rem)]" style="--i:1">Welcome <span class="text-accent">back.</span></h1>
      <p class="rise mt-4 text-[16px] leading-relaxed text-muted" style="--i:2">Sign in to your Futureproof account.</p>

      <p *ngIf="sessionExpired" class="rise mt-6 rounded-xl border border-band-moderate px-4 py-3 text-[14px] text-band-moderate" style="--i:2">
        Your session has expired. Please sign in again.
      </p>
      <form [formGroup]="form" (ngSubmit)="login()" class="card rise mt-8 space-y-5 p-6 sm:p-8" style="--i:3" novalidate>
        <div>
          <label for="email" class="mb-2 block text-[14px] font-medium text-muted">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" class="field" placeholder="you@example.com" />
          <p *ngIf="form.controls.email.touched && form.controls.email.invalid" class="mt-2 text-[13px] text-band-very-high">Please enter a valid email</p>
        </div>
        <div>
          <label for="password" class="mb-2 block text-[14px] font-medium text-muted">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" class="field" placeholder="Your password" />
          <p *ngIf="form.controls.password.touched && form.controls.password.invalid" class="mt-2 text-[13px] text-band-very-high">Password is required</p>
        </div>

        <p *ngIf="error" class="rounded-xl border border-band-very-high px-4 py-3 text-[14px] text-band-very-high" role="alert">{{ error }}</p>

        <button type="submit" [disabled]="loading" class="btn-accent w-full">
          <span *ngIf="!loading">Sign in</span>
          <span *ngIf="loading">Signing in…</span>
          <span *ngIf="!loading" class="arrow" aria-hidden="true">&rarr;</span>
        </button>
      </form>

      <p class="rise mt-6 text-center text-[14px] text-muted" style="--i:4">
        Don't have an account?
        <a routerLink="/register" class="ml-1 font-semibold text-accent hover:underline">Create one</a>
      </p>
    </div>
  `
})
export class LoginPageComponent {
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  loading = false;
  error = '';
  sessionExpired = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.sessionExpired = this.route.snapshot.queryParams['reason'] === 'session-expired';
  }

  login(): void {
    if (this.form.invalid) {
      this.error = '';
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    this.auth.login({
      email: this.form.value.email || '',
      password: this.form.value.password || ''
    }).subscribe({
      next: () => {
        this.router.navigate(['/ai-test']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Invalid email or password';
      }
    });
  }
}
