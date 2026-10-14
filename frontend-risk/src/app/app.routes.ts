import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const appRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/home/home.page').then(m => m.HomePageComponent)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.page').then(m => m.LoginPageComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/login/register.page').then(m => m.RegisterPageComponent)
  },
  {
    path: 'ai-test',
    loadComponent: () =>
      import('./pages/ai-check/ai-check.page').then(m => m.AiCheckPageComponent)
  },
  {
    path: 'privacy',
    title: 'Privacy policy – Futureproof',
    loadComponent: () =>
      import('./pages/legal/privacy.page').then(m => m.PrivacyPageComponent)
  },
  {
    path: 'terms',
    title: 'Terms of use – Futureproof',
    loadComponent: () =>
      import('./pages/legal/terms.page').then(m => m.TermsPageComponent)
  },
  {
    path: 'risk',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/risk/risk.page').then(m => m.RiskPageComponent)
  },
  { path: '**', redirectTo: '' }
];
