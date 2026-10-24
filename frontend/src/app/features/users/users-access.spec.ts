import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { routes } from '../../app.routes';
import { LayoutComponent } from '../../layout/components/layout/layout.component';
import { CrmNavigation } from '../../core/navigation/crm-navigation';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { safeReturnUrl } from '../../core/services/auth-navigation';

describe('users route and navigation', () => {
  for (const role of Object.values(CrmRole)) {
    it('protects the route and menu for ' + role, async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), { provide: AuthService,
        useValue: { hasAnyRole: (roles: CrmRole[]) => roles.includes(role), ensureSession: () => of(true) } }] });
      const route = routes.find(route => route.component === LayoutComponent)!.children!.find(route => route.path === 'users')!;
      expect(route.canActivate).toEqual([authGuard, roleGuard]); expect(route.loadComponent).toBeDefined();
      const allowed = [CrmRole.ADMIN, CrmRole.SUPER_ADMIN].includes(role);
      expect(TestBed.inject(CrmNavigation).items().some(item => item.route === '/users')).toBe(allowed);
      const result = await firstValueFrom(TestBed.runInInjectionContext(() => roleGuard(
        { data: route.data } as ActivatedRouteSnapshot, { url: '/users' } as RouterStateSnapshot)) as Observable<boolean | UrlTree>);
      if (allowed) expect(result).toBeTrue();
      else expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
      expect(safeReturnUrl('/users')).toBe('/users');
    });
  }
});
