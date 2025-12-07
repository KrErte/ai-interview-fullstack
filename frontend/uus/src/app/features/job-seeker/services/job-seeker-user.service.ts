import { Injectable } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

export interface JobSeekerUser {
  id: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class JobSeekerUserService {
  constructor(private auth: AuthService) {}

  /** Returns a minimal mock user object from AuthService (no backend call). */
  getCurrentUser(): JobSeekerUser | null {
    const email = this.auth.getCurrentUserEmail();
    if (!email) return null;
    // For a real app, parse from JWT or fetch from backend
    return {
      id: email,
      name: email.split('@')[0], // Placeholder: just email user part
      email: email
    };
  }
}
