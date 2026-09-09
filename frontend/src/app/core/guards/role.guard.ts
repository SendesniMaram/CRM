import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CrmRole } from '../models/role.model';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    return inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  const requiredRoles = (route.data?.['roles'] ?? []) as CrmRole[];

  if (requiredRoles.length === 0 || authService.hasAnyRole(requiredRoles)) {
    return true;
  }

  return inject(Router).createUrlTree(['/access-denied']);
};