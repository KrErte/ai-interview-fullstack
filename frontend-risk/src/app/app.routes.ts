import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

// Every route sets a title: Angular only updates the tab title on routes that have one, so a route
// without it would keep the previous page's title (e.g. "Privacy policy") after navigation.
const HOME_TITLE = 'Futureproof – The AI roadmap for your job';

export const appRoutes: Routes = [
  {
    path: '',
    title: HOME_TITLE,
    loadComponent: () =>
      import('./pages/home/home.page').then(m => m.HomePageComponent)
  },
  {
    path: 'login',
    title: 'Log in – Futureproof',
    loadComponent: () =>
      import('./pages/login/login.page').then(m => m.LoginPageComponent)
  },
  {
    path: 'register',
    title: 'Create an account – Futureproof',
    loadComponent: () =>
      import('./pages/login/register.page').then(m => m.RegisterPageComponent)
  },
  {
    path: 'ai-test',
    title: 'Find your starting point – Futureproof',
    loadComponent: () =>
      import('./pages/ai-check/ai-check.page').then(m => m.AiCheckPageComponent)
  },
  {
    path: 'why',
    title: 'Why I built Futureproof',
    loadComponent: () =>
      import('./pages/why/why.page').then(m => m.WhyPageComponent)
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
    title: HOME_TITLE,
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/risk/risk.page').then(m => m.RiskPageComponent)
  },
  { path: '**', redirectTo: '' }
];
