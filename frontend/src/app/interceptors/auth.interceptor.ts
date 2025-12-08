import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Adds the Authorization header to every outgoing request when a token exists.
 * Does not override an existing Authorization header.
 * Uses 'token' key to match AuthService storage.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Use 'token' key to match AuthService.TOKEN_KEY
  const token = localStorage.getItem('token');

  // Skip if no token, Authorization header already exists, or request is to auth endpoints
  if (!token || req.headers.has('Authorization') || req.url.includes('/auth')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};