import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { computed, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { CrmRole } from '../models/role.model';
import { storeTestSession, testJwt, testLogin } from '../testing/auth-fixtures';
import { jwtExpiration } from './jwt-expiration';

describe('AuthService', () => {
  const base = 'http://localhost:8080/identity/api/auth';
  let http: HttpTestingController;
  let router: Router;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      provideRouter([]), provideZonelessChangeDetection()] });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });
  const service = () => TestBed.inject(AuthService);

  it('logs in using the exact contract and stores tokens and user reactively', () => {
    const auth = service();
    const connected = computed(() => auth.isAuthenticated());
    expect(connected()).toBeFalse();
    const request = { username: 'unit-user', email: null, password: 'fictitious-password' };
    const response = testLogin();
    auth.login(request).subscribe();
    const login = http.expectOne(base + '/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual(request);
    login.flush(response);
    expect(connected()).toBeTrue();
    expect(auth.getToken()).toBe(response.token);
    expect(auth.getCurrentUser()?.username).toBe('unit-user');
    const stored = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    expect(stored.accessToken).toBe(response.token);
    expect(stored.refreshToken).toBe(response.refreshToken);
    expect(stored.user.roles).toEqual([CrmRole.ADMIN]);
    expect(auth.hasRole(CrmRole.ADMIN)).toBeTrue();
    expect(auth.hasAnyRole([CrmRole.CLIENT, CrmRole.ADMIN])).toBeTrue();
    expect(auth.hasRole(CrmRole.CLIENT)).toBeFalse();
  });

  it('recognizes an unexpired token restored from storage', () => {
    storeTestSession();
    expect(service().isAuthenticated()).toBeTrue();
  });

  it('reads exp from a valid UTF-8 payload containing non-ASCII characters', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const bytes = new TextEncoder().encode(JSON.stringify({ exp, name: 'Unit \u00e9\u6c49\ud83e\udd8a' }));
    const payload = btoa(String.fromCharCode(...bytes))
      .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    const token = testJwt().split('.')[0] + '.' + payload + '.fictitious';
    expect(jwtExpiration(token)).toBe(exp * 1000);
    storeTestSession(token);
    expect(service().isAuthenticated()).toBeTrue();
    expect(service().getCurrentUser()?.username).toBe('unit-user');
    http.expectNone(base + '/refresh');
  });

  const header = testJwt().split('.')[0];
  for (const [name, token] of [
    ['malformed token', 'malformed'], ['invalid base64', header + '.!.b'],
    ['undecodable payload', header + '._w.b'], ['non-JSON payload', header + '.bm90LWpzb24.b'],
    ['invalid header', 'a.' + testJwt().split('.')[1] + '.fictitious'],
    ['missing exp', testJwt({ sub: 'unit-user' })], ['string exp', testJwt({ exp: '9999999999' })],
    ['expired token', testJwt({ exp: 1 })], ['null payload', header + '.bnVsbA.b']
  ]) {
    it('rejects ' + name + ' without crashing', () => {
      storeTestSession(token);
      expect(service().isAuthenticated()).toBeFalse();
      expect(service().getCurrentUser()).toBeNull();
    });
  }

  it('handles absent tokens', () => {
    expect(service().isAuthenticated()).toBeFalse();
    expect(service().getRoles()).toEqual([]);
  });

  it('removes malformed stored JSON', () => {
    sessionStorage.setItem('crm.auth.session', '{broken');
    expect(service().getToken()).toBeNull();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });

  it('updates computed authentication when exp is reached', () => {
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date('2030-01-01T00:00:00Z'));
      storeTestSession(testJwt({ exp: Date.now() / 1000 + 1 }));
      const connected = computed(() => service().isAuthenticated());
      expect(connected()).toBeTrue();
      jasmine.clock().tick(1000);
      expect(connected()).toBeFalse();
    } finally { jasmine.clock().uninstall(); }
  });

  it('refreshes an expired session before allowing navigation and updates roles', () => {
    storeTestSession(testJwt({ exp: 1 }));
    const auth = service();
    let result: boolean | undefined;
    auth.ensureSession().subscribe((value) => result = value);
    const refresh = http.expectOne(base + '/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'fictitious-refresh' });
    refresh.flush({ accessToken: testJwt(), refreshToken: 'fictitious-rotated',
      roles: [CrmRole.CLIENT], type: 'Bearer', expiration: 3600000 });
    expect(result).toBeTrue();
    expect(auth.getRoles()).toEqual([CrmRole.CLIENT]);
    expect(JSON.parse(sessionStorage.getItem('crm.auth.session')!).refreshToken).toBe('fictitious-rotated');
  });

  it('clears an expired session when refresh fails during navigation', () => {
    storeTestSession(testJwt({ exp: 1 }));
    let allowed: boolean | undefined;
    service().ensureSession().subscribe((value) => allowed = value);
    http.expectOne(base + '/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(allowed).toBeFalse();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });

  it('clears an expired session with no refresh token', () => {
    storeTestSession(testJwt({ exp: 1 }), '');
    service().ensureSession().subscribe((value) => expect(value).toBeFalse());
    http.expectNone(base + '/refresh');
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });

  it('logs out locally, clears all auth data and immediately updates state', () => {
    storeTestSession();
    const auth = service();
    const connected = computed(() => auth.isAuthenticated());
    expect(connected()).toBeTrue();
    auth.logout();
    expect(connected()).toBeFalse();
    expect(auth.getCurrentUser()).toBeNull();
    expect(auth.getToken()).toBeNull();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    http.expectNone(base + '/logout');
  });

  it('does not restore a session if refresh completes after logout', () => {
    storeTestSession();
    const auth = service();
    auth.refreshAccessToken().subscribe({ error: () => {} });
    const pending = http.expectOne(base + '/refresh');
    auth.logout();
    pending.flush({ accessToken: testJwt(), refreshToken: 'fictitious-rotated',
      roles: [CrmRole.ADMIN], type: 'Bearer', expiration: 3600000 });
    expect(auth.isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });

  it('does not restore a session if login completes after logout', () => {
    const auth = service();
    auth.login({ username: 'unit', email: null, password: 'fictitious' }).subscribe({ error: () => {} });
    const pending = http.expectOne(base + '/login');
    auth.logout();
    pending.flush(testLogin());
    expect(auth.isAuthenticated()).toBeFalse();
  });

  it('rejects an invalid token in a login response', () => {
    service().login({ username: 'unit', email: null, password: 'fictitious' }).subscribe({ error: () => {} });
    http.expectOne(base + '/login').flush({ ...testLogin(), token: 'malformed' });
    expect(service().isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });
});
