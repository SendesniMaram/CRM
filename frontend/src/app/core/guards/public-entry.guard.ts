import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { isSessionUnavailable } from '../services/session-unavailable';

/** The private home already presents only the tools authorized for each role. */
export const publicEntryGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  // A first visit or explicit logout needs no refresh and no expiry notice.
  if (!auth.getToken()) return true;
  return auth.ensureSession().pipe(
    map(authenticated => authenticated ? router.createUrlTree(['/home']) : true),
    // A public page remains available during an outage, without clearing the
    // stored session or granting access to any private route.
    catchError(error => isSessionUnavailable(error) ? of(true) : throwError(() => error))
  );
};
