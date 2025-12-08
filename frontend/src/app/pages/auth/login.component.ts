import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../services/auth.service';
import { AuthLayoutComponent } from '../../layouts/auth/auth-layout.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AuthLayoutComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });
  loading = false;
  error = '';
  sessionExpired = false;
  showPassword = false;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    const reason = this.route.snapshot.queryParamMap.get('reason');
    if (reason === 'session-expired') {
      this.sessionExpired = true;
      this.error = 'Sessioon aegus. Palun logi uuesti sisse.';
    }
  }

  submit() {
    if (this.loading) return;
    this.error = '';
    this.sessionExpired = false;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload: LoginRequest = {
      email: this.form.value.email!,
      password: this.form.value.password!
    };

    this.auth.login(payload).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        // Extract error message from backend response (400/401/403)
        const status = err?.status;
        if (status === 400 || status === 401 || status === 403) {
          this.error = err?.error?.message || err?.error?.error || 'Invalid email or password.';
        } else {
          this.error = err?.error?.message || err?.message || 'Login failed. Please try again.';
        }
      }
    });
  }
}
