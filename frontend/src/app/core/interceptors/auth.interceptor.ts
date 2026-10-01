import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const url = new URL(request.url, window.location.origin);
  // Never send CRM credentials to third-party URLs or intercept public auth calls.
  if (url.origin !== 'http://localhost:8080' || !/^\/[a-z-]+\/api\//.test(url.pathname)
      || url.pathname.startsWith('/identity/api/auth/')) return next(request);

  const auth = inject(AuthService);
  const router = inject(Router);
  const generation = auth.sessionGeneration;
  const revision = auth.tokenRevision;
  const token = auth.getToken();
  const authorized = (value: string | null) => value
    ? request.clone({ setHeaders: { Authorization: 'Bearer ' + value } }) : request;
  const fail = (error: HttpErrorResponse) => {
    if (generation === auth.sessionGeneration) {
      if (error.status === 401) auth.expireSession(router.url, generation);
      else if (error.status === 403) void router.navigate(['/access-denied']);
    }
    return throwError(() => error);
  };

  return next(authorized(token)).pipe(catchError((error: HttpErrorResponse) => {
    if (error.status !== 401 || generation !== auth.sessionGeneration) return fail(error);
    // A delayed 401 for the previous JWT reuses the token already refreshed.
    const current = auth.getToken();
    const refresh = current && auth.tokenRevision !== revision && auth.isAuthenticated()
      ? of(current) : auth.refreshAccessToken();
    return refresh.pipe(
      catchError((refreshError) => {
        auth.expireSession(router.url, generation);
        return throwError(() => refreshError);
      }),
      switchMap((newToken) => next(authorized(newToken)).pipe(catchError(fail)))
    );
  }));
};
