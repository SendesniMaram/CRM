import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CrmRole } from '../models/role.model';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);

  const router = inject(Router);
  return authService.ensureSession().pipe(map((authenticated) => {
    if (!authenticated) {
      return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
      });
    }

    const requiredRoles = (route.data?.['roles'] ?? []) as CrmRole[];

    if (requiredRoles.length === 0 || authService.hasAnyRole(requiredRoles)) {
      return true;
    }

    return router.createUrlTree(['/access-denied']);
  }));
};
