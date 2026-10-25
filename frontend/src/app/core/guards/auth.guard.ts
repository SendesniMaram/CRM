import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, map, of, throwError } from 'rxjs';
import { isSessionUnavailable } from '../services/session-unavailable';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);

  const router = inject(Router);
  return authService.ensureSession().pipe(map((authenticated) => authenticated || router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  })), catchError(error => isSessionUnavailable(error) ? of(false) : throwError(() => error)));
};
