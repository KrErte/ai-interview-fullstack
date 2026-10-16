import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

// Every route sets a title: Angular only updates the tab title on routes that have one, so a route
// without it would keep the previous page's title (e.g. "Privacy policy") after navigation.
const HOME_TITLE = 'careerrisk.ee – The AI roadmap for your job';

export const appRoutes: Routes = [
  {
    path: '',
    title: HOME_TITLE,
    loadComponent: () =>
      import('./pages/home/home.page').then(m => m.HomePageComponent)
  },
  {
    path: 'login',
    title: 'Log in – careerrisk.ee',
    loadComponent: () =>
      import('./pages/login/login.page').then(m => m.LoginPageComponent)
  },
  {
    path: 'register',
    title: 'Create an account – careerrisk.ee',
    loadComponent: () =>
      import('./pages/login/register.page').then(m => m.RegisterPageComponent)
  },
  {
    path: 'ai-test',
    title: 'Find your starting point – careerrisk.ee',
    loadComponent: () =>
      import('./pages/ai-check/ai-check.page').then(m => m.AiCheckPageComponent)
  },
  {
    path: 'teams',
    title: 'Team AI report – careerrisk.ee',
    loadComponent: () =>
      import('./pages/teams/teams.page').then(m => m.TeamsPageComponent)
  },
  {
    // the report token is in the URL fragment (#token), see TeamReportPageComponent
    path: 'teams/report',
    title: 'Team report – careerrisk.ee',
    loadComponent: () =>
      import('./pages/teams/team-report.page').then(m => m.TeamReportPageComponent)
  },
  {
    path: 'why',
    title: 'Why I built careerrisk.ee',
    loadComponent: () =>
      import('./pages/why/why.page').then(m => m.WhyPageComponent)
  },
  {
    path: 'privacy',
    title: 'Privacy policy – careerrisk.ee',
    loadComponent: () =>
      import('./pages/legal/privacy.page').then(m => m.PrivacyPageComponent)
  },
  {
    path: 'terms',
    title: 'Terms of use – careerrisk.ee',
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
