import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (!token || !needsToken(req.url)) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        authService.logout();
        router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
      }
      return throwError(() => error);
    })
  );
};

/**
 * Auth endpoints and the public AI readiness test don't get the token; everything else does,
 * including /api/billing/* and the paid POST /api/ai-readiness/roadmap.
 */
export function needsToken(url: string): boolean {
  const path = url.split('?')[0];
  if (/\/api\/ai-readiness\/roadmap\/?$/.test(path)) {
    return true;
  }
  return !path.includes('/auth') && !path.includes('/api/ai-readiness');
}
