import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);

  const router = inject(Router);
  return authService.ensureSession().pipe(map((authenticated) => authenticated || router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  })));
};
