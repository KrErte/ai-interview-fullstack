import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent {

  mode: 'login' | 'register' = 'login';

  email = '';
  password = '';
  fullName = '';

  loading = false;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  switchMode(mode: 'login' | 'register') {
    this.mode = mode;
    this.error = null;
  }

  submit() {
    this.loading = true;
    this.error = null;

    if (this.mode === 'register') {
      this.authService.register({
        email: this.email,
        password: this.password,
        fullName: this.fullName
      }).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/']);
        },
        error: () => {
          this.loading = false;
          this.error = 'Registreerimine ebaõnnestus';
        }
      });
    } else {
      this.authService.login({
        email: this.email,
        password: this.password
      }).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/']);
        },
        error: () => {
          this.loading = false;
          this.error = 'Vale email või parool';
        }
      });
    }
  }
}
