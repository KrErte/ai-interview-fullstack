import { Routes } from '@angular/router';

export const skillMatrixRoutes: Routes = [
  {
    path: 'start',
    loadComponent: () =>
      import(
        './pages/skill-matrix-start-page/skill-matrix-start-page.component'
      ).then(m => m.SkillMatrixStartPageComponent),
  },
  {
    path: 'session/:sessionId',
    loadComponent: () =>
      import(
        './pages/skill-matrix-session-page/skill-matrix-session-page.component'
      ).then(m => m.SkillMatrixSessionPageComponent),
  },
  {
    path: 'results/:sessionId',
    loadComponent: () =>
      import(
        './pages/skill-matrix-results-page/skill-matrix-results-page.component'
      ).then(m => m.SkillMatrixResultsPageComponent),
  },
];


