import { Routes } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/auth/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'upload-cv',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/upload-cv/upload-cv.component').then(m => m.UploadCvComponent)
  },
  {
    path: 'job-match',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/job-match/job-match.component').then(m => m.JobMatchComponent)
  },
  {
    path: 'job-analysis',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/job-analysis/job-analysis.component').then(m => m.JobAnalysisComponent)
  },
  {
    path: 'training',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/training/training.component').then(m => m.TrainingComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/profile/profile.component').then(m => m.ProfileComponent)
  },
  { path: '**', redirectTo: 'dashboard' },
];

export default provideRouter(appRoutes);
