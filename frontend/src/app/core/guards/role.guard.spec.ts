import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CrmRole } from '../models/role.model';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'hasAnyRole']);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authService }
      ]
    });
  });

  it('allows an authenticated user with an accepted role', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.hasAnyRole.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => roleGuard(
      { data: { roles: [CrmRole.ADMIN] } } as never,
      { url: '/employees' } as never
    ));

    expect(result).toBeTrue();
  });

  it('redirects an authenticated user without the required role', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.hasAnyRole.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => roleGuard(
      { data: { roles: [CrmRole.ADMIN] } } as never,
      { url: '/employees' } as never
    ));

    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/access-denied']));
  });

  it('redirects unauthenticated users to login with the returnUrl', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => roleGuard(
      { data: { roles: [CrmRole.ADMIN] } } as never,
      { url: '/employees' } as never
    ));

    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: '/employees' }
    }));
  });

  it('allows an authenticated user when roles metadata is absent or empty', () => {
    authService.isAuthenticated.and.returnValue(true);

    expect(TestBed.runInInjectionContext(() => roleGuard(
      { data: {} } as never,
      { url: '/dashboard' } as never
    ))).toBeTrue();
    expect(TestBed.runInInjectionContext(() => roleGuard(
      { data: { roles: [] } } as never,
      { url: '/dashboard' } as never
    ))).toBeTrue();
    expect(authService.hasAnyRole).not.toHaveBeenCalled();
  });

  it('allows access when any one of several required roles matches', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.hasAnyRole.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => roleGuard(
      { data: { roles: [CrmRole.ADMIN, CrmRole.SUPER_ADMIN] } } as never,
      { url: '/administration' } as never
    ));

    expect(result).toBeTrue();
    expect(authService.hasAnyRole).toHaveBeenCalledWith([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]);
  });
});