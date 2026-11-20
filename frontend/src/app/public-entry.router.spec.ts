import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { CrmRole } from './core/models/role.model';
import { AuthService } from './core/services/auth.service';
import { storeTestSession, testJwt, testLogin } from './core/testing/auth-fixtures';
import { LandingComponent } from './features/landing/landing.component';

describe('Application public entry with real routing and sessions', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(),
      provideRouter(routes), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });

  for (const url of ['/', '/welcome']) {
    it(`shows the existing public page at ${url} without API requests`, async () => {
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl(url, LandingComponent);
      expect(TestBed.inject(Router).url).toBe(url);
      expect(harness.routeNativeElement?.querySelector('.hero a[href="/login"]')).not.toBeNull();
      expect(harness.routeNativeElement?.querySelector('figcaption')?.textContent).toContain('Données fictives');
      expect(harness.routeNativeElement?.querySelector('app-layout')).toBeNull();
      expect(TestBed.inject(AuthService).sessionNotice()).toBe('');
      http.expectNone(() => true);
    });
  }

  for (const role of Object.values(CrmRole)) {
    for (const url of ['/', '/welcome', '/login']) {
      it(`redirects ${role} from ${url} to its authorized home`, async () => {
        const { token, refreshToken, ...user } = testLogin();
        sessionStorage.setItem('crm.auth.session', JSON.stringify({ accessToken: token, refreshToken,
          user: { ...user, role, roles: [role] } }));
        const harness = await RouterTestingHarness.create(url);
        expect(TestBed.inject(Router).url).toBe('/home');
        expect(harness.routeNativeElement?.querySelector('app-home')).not.toBeNull();
        const admin = role === CrmRole.SUPER_ADMIN || role === CrmRole.ADMIN;
        expect(!!harness.routeNativeElement?.querySelector('a[href="/employees"]')).toBe(admin);
        expect(!!harness.routeNativeElement?.querySelector('a[href="/client"]')).toBe(role === CrmRole.CLIENT);
        expect(!!harness.routeNativeElement?.querySelector('a[href="/profile"]')).toBe(role === CrmRole.EMPLOYEE);
        http.expectNone(() => true);
      });
    }
  }

  for (const url of ['/home', '/dashboard', '/employees', '/departments', '/customers', '/leaves', '/hr-insights', '/administration']) {
    it(`keeps direct guest access to ${url} protected`, async () => {
      const harness = await RouterTestingHarness.create(url);
      const router = TestBed.inject(Router);
      expect(router.url.split('?')[0]).toBe('/login');
      expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe(url);
      expect(harness.routeNativeElement?.querySelector('app-home')).toBeNull();
      http.expectNone(() => true);
    });
  }

  it('refreshes an expired session before entering the private home', async () => {
    storeTestSession(testJwt({ exp: 1 }));
    const harness = await RouterTestingHarness.create();
    const pending = harness.navigateByUrl('/');
    await Promise.resolve();
    await new Promise(resolve => setTimeout(resolve, 0));
    http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({
      accessToken: testJwt(), refreshToken: 'fictitious-rotated', roles: [CrmRole.ADMIN]
    });
    await pending;
    expect(TestBed.inject(Router).url).toBe('/home');
  });

  for (const status of [401, 503]) {
    it(`keeps the public page available when session refresh returns ${status}`, async () => {
      storeTestSession(testJwt({ exp: 1 }));
      const stored = sessionStorage.getItem('crm.auth.session');
      const harness = await RouterTestingHarness.create();
      const pending = harness.navigateByUrl('/');
      await new Promise(resolve => setTimeout(resolve, 0));
      http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({}, { status, statusText: 'Test failure' });
      await pending;
      expect(TestBed.inject(Router).url).toBe('/');
      expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('clarté');
      expect(sessionStorage.getItem('crm.auth.session')).toBe(status === 503 ? stored : null);
      http.expectNone(request => !request.url.endsWith('/refresh'));
    });
  }

  it('clears logout, returns to public entry and still refuses private deep links', async () => {
    storeTestSession();
    const harness = await RouterTestingHarness.create('/');
    const router = TestBed.inject(Router);
    const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
    TestBed.inject(AuthService).logout();
    http.expectOne('http://localhost:8080/identity/api/auth/logout').flush(null);
    await navigation;
    expect(router.url).toBe('/login');
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
    await harness.navigateByUrl('/', LandingComponent);
    await harness.navigateByUrl('/employees');
    expect(router.url).toBe('/login?returnUrl=%2Femployees');
  });
});
