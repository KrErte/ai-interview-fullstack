import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Adds the Authorization header to every outgoing request when a token exists.
 * Does not override an existing Authorization header.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('authToken');

  if (!token || req.headers.has('Authorization')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};