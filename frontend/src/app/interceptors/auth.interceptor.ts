import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Attaches JWT from localStorage to all /api/** requests as Authorization header.
 * Skips when no token, when a header already exists, or for auth endpoints.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('auth_token');

  const isApiRequest = req.url.includes('/api/');
  const isAuthRequest = req.url.includes('/auth');
  const hasAuthHeader = req.headers.has('Authorization');

  if (!token || !isApiRequest || hasAuthHeader || isAuthRequest) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};