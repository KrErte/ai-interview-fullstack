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
    path: 'risk',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/risk/risk.page').then(m => m.RiskPageComponent)
  },
  { path: '**', redirectTo: '' }
];
