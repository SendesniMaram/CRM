import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { authGuard } from './auth.guard';
import { storeTestSession, testJwt } from '../testing/auth-fixtures';
import { CrmRole } from '../models/role.model';

describe('authGuard', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      provideRouter([]), provideZonelessChangeDetection()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });
  function run(url = '/dashboard') {
    return firstValueFrom(TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url } as never)) as Observable<boolean | UrlTree>);
  }
  it('redirects an absent session to login with returnUrl', async () => {
    expect(await run()).toEqual(TestBed.inject(Router).createUrlTree(['/login'],
      { queryParams: { returnUrl: '/dashboard' } }));
  });
  it('allows a valid token without refresh', async () => {
    storeTestSession();
    expect(await run()).toBeTrue();
  });
  it('preserves query parameters in returnUrl', async () => {
    expect(await run('/dashboard?view=summary')).toEqual(TestBed.inject(Router).createUrlTree(['/login'],
      { queryParams: { returnUrl: '/dashboard?view=summary' } }));
  });
  it('waits for refresh of an expired token before allowing the protected route', async () => {
    storeTestSession(testJwt({ exp: 1 }));
    const pending = run('/employees');
    http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({
      accessToken: testJwt(), refreshToken: 'fictitious-new', roles: [CrmRole.ADMIN],
      type: 'Bearer', expiration: 3600000
    });
    expect(await pending).toBeTrue();
  });
  it('redirects to login after refresh failure and preserves the requested route', async () => {
    storeTestSession(testJwt({ exp: 1 }));
    const pending = run('/employees/12');
    http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(await pending).toEqual(TestBed.inject(Router).createUrlTree(['/login'],
      { queryParams: { returnUrl: '/employees/12' } }));
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });
});
