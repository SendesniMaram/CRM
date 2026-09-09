import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    sessionStorage.clear();
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authService }
      ]
    });
  });

  it('should redirect unauthenticated users to login', () => {
    authService.isAuthenticated.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/dashboard' } as never)
    );

    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: '/dashboard' }
    }));
  });

  it('allows access when a session is valid', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/dashboard' } as never)
    );

    expect(result).toBeTrue();
  });

  it('preserves the requested returnUrl when redirecting', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/dashboard?view=summary' } as never)
    );

    expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: '/dashboard?view=summary' }
    }));
  });
});
