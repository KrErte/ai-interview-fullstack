import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'interviewer',
    loadComponent: () =>
      import('./interviewer/interviewer.component').then(m => m.InterviewerComponent)
  },
  {
    path: 'candidate',
    loadComponent: () =>
      import('./candidate/candidate.component').then(m => m.CandidateComponent)
  },
  {
    path: 'candidate-profile',
    loadComponent: () =>
      import('./candidate-profile/candidate-profile.component')
        .then(m => m.CandidateProfileComponent)
  },
  {
    path: 'job-matcher',
    loadComponent: () =>
      import('./candidate-job-matcher/candidate-job-matcher.component')
        .then(m => m.CandidateJobMatcherComponent)
  },
  {
    path: 'trainer',
    loadComponent: () =>
      import('./question-trainer/question-trainer.component')
        .then(m => m.QuestionTrainerComponent)
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./auth/auth.component').then(m => m.AuthComponent)
  },
  { path: '**', redirectTo: '' },
  {
  path: 'profile',
  loadComponent: () =>
    import('./candidate-profile/candidate-profile.component').then(
      (m) => m.CandidateProfileComponent
    ),
}

];
