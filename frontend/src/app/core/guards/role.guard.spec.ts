import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { CrmRole } from '../models/role.model';
import { roleGuard } from './role.guard';
import { storeTestSession, testJwt } from '../testing/auth-fixtures';

describe('roleGuard', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      provideRouter([]), provideZonelessChangeDetection()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });
  const run = (roles?: CrmRole[]) => firstValueFrom(TestBed.runInInjectionContext(() =>
    roleGuard({ data: { roles } } as never, { url: '/employees' } as never)) as Observable<boolean | UrlTree>);

  it('allows an authenticated user with an accepted role', async () => {
    storeTestSession();
    expect(await run([CrmRole.ADMIN, CrmRole.SUPER_ADMIN])).toBeTrue();
  });
  it('redirects insufficient roles to access-denied without clearing session', async () => {
    storeTestSession();
    expect(await run([CrmRole.CLIENT])).toEqual(TestBed.inject(Router).createUrlTree(['/access-denied']));
    expect(sessionStorage.getItem('crm.auth.session')).not.toBeNull();
  });
  it('redirects unauthenticated users with returnUrl', async () => {
    expect(await run([CrmRole.ADMIN])).toEqual(TestBed.inject(Router).createUrlTree(['/login'],
      { queryParams: { returnUrl: '/employees' } }));
  });
  it('allows authenticated users without roles metadata', async () => {
    storeTestSession();
    expect(await run()).toBeTrue();
    expect(await run([])).toBeTrue();
  });
  it('checks refreshed roles rather than stale roles', async () => {
    storeTestSession(testJwt({ exp: 1 }));
    const result = run([CrmRole.ADMIN]);
    http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({
      accessToken: testJwt(), refreshToken: 'fictitious-new', roles: [CrmRole.CLIENT],
      type: 'Bearer', expiration: 3600000
    });
    expect(await result).toEqual(TestBed.inject(Router).createUrlTree(['/access-denied']));
  });
});
