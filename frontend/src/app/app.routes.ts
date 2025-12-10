import { Routes } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard, loginRedirectGuard } from './guards/auth.guard';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    canActivate: [loginRedirectGuard],
    loadComponent: () =>
      import('./auth/login/login.component').then(m => m.LoginComponent)
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
    path: 'soft-skills',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/soft-skills/soft-skill-merger-page.component').then(
        m => m.SoftSkillMergerPageComponent
      )
  },
  {
    path: 'skill-matrix',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./skill-matrix/skill-matrix.routes').then(
        m => m.skillMatrixRoutes
      ),
  },
  {
    path: 'interview-studio',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/mvp/interview-studio-page.component').then(m => m.InterviewStudioPageComponent)
  },
  {
    path: 'interview-deja-vu',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/mvp/interview-deja-vu-page.component').then(m => m.InterviewDejaVuPageComponent)
  },
  {
    path: 'story-forge',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/mvp/story-forge-page.component').then(m => m.StoryForgePageComponent)
  },
  {
    path: 'career-twin',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/mvp/career-twin-page.component').then(m => m.CareerTwinPageComponent)
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
