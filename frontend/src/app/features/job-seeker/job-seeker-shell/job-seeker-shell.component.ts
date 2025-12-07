import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { JobSeekerSidebarComponent } from './job-seeker-sidebar.component';
import { JobSeekerUserService, JobSeekerUser } from '../services/job-seeker-user.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-job-seeker-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, JobSeekerSidebarComponent],
  templateUrl: './job-seeker-shell.component.html',
  styleUrls: ['./job-seeker-shell.component.scss']
})
export class JobSeekerShellComponent {
  user: JobSeekerUser | null;
  userName = '';
  userInitials = '';
  userMenuOpen = false;

  constructor(
    private userService: JobSeekerUserService,
    private authService: AuthService,
    private router: Router
  ) {
    this.user = this.userService.getCurrentUser();
    if (this.user) {
      this.userName = this.user.name || this.user.email.split('@')[0];
      this.userInitials = this.generateInitials(this.user.name || this.user.email);
    }
  }

  generateInitials(input: string): string {
    // If full name, get initials; else use email user part
    const parts = input.trim().split(/\s+/);
    if (parts.length >= 2) {
      return parts[0][0].toUpperCase() + parts[1][0].toUpperCase();
    }
    return input[0]?.toUpperCase() ?? '';
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
