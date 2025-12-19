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
    <div class="min-h-screen flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <!-- Logo -->
        <div class="text-center mb-8">
          <div class="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 shadow-xl shadow-emerald-500/25 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-slate-100">Create your account</h1>
          <p class="text-slate-500 mt-2">Start your career risk assessment journey</p>
        </div>

        <!-- Register Form -->
        <div class="rounded-3xl border border-slate-800/50 bg-gradient-to-br from-slate-900/80 to-slate-900/40 p-8 backdrop-blur-sm">
          <form [formGroup]="form" (ngSubmit)="register()" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-2">Full Name</label>
              <input
                type="text"
                formControlName="fullName"
                class="w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none transition-all"
                placeholder="John Doe"
              />
              <p *ngIf="form.controls.fullName.touched && form.controls.fullName.invalid" class="text-rose-400 text-sm mt-2">
                Full name is required
              </p>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-300 mb-2">Email</label>
              <input
                type="email"
                formControlName="email"
                class="w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none transition-all"
                placeholder="you@example.com"
              />
              <p *ngIf="form.controls.email.touched && form.controls.email.invalid" class="text-rose-400 text-sm mt-2">
                Please enter a valid email
              </p>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-300 mb-2">Password</label>
              <input
                type="password"
                formControlName="password"
                class="w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none transition-all"
                placeholder="Create a strong password"
              />
              <p *ngIf="form.controls.password.touched && form.controls.password.invalid" class="text-rose-400 text-sm mt-2">
                Password must be at least 6 characters
              </p>
            </div>

            <div *ngIf="error" class="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20">
              <p class="text-rose-300 text-sm">{{ error }}</p>
            </div>

            <button
              type="submit"
              [disabled]="loading"
              class="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-4 font-semibold text-white shadow-lg shadow-emerald-500/25 transition-all hover:shadow-emerald-500/40 hover:scale-[1.01] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            >
              <span *ngIf="!loading">Create Account</span>
              <span *ngIf="loading" class="flex items-center justify-center gap-2">
                <svg class="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Creating account...
              </span>
            </button>
          </form>

          <div class="mt-6 text-center">
            <p class="text-slate-500 text-sm">
              Already have an account?
              <a routerLink="/login" class="text-emerald-400 hover:text-emerald-300 transition-colors ml-1">
                Sign in
              </a>
            </p>
          </div>
        </div>
      </div>
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
        this.router.navigate(['/risk']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
