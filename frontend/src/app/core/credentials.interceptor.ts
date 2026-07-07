import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Ensures the session cookie (and XSRF cookie) travel with every API call even when
 * the frontend isn't served through the dev proxy — e.g. a production deployment where
 * Angular and the backend are genuinely different origins behind CORS.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  return next(req.clone({ withCredentials: true }));
};
