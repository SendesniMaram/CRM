import { dashboardNavigationStub } from '../../../core/testing/dashboard-fixture';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom } from 'rxjs';
import { routes } from '../../../app.routes';
import { authInterceptor } from '../../../core/interceptors/auth.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { testLogin } from '../../../core/testing/auth-fixtures';
import { LoginComponent } from './login.component';
import { CrmRole } from '../../../core/models/role.model';

describe('Login returnUrl with application routes and real AuthService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideZonelessChangeDetection(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(), dashboardNavigationStub
    ] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    sessionStorage.clear();
  });

  for (const destination of ['/home', '/customers/new', '/dashboard?view=summary#stats', '/departments', '/departments/new', '/departments/12/edit', '/login']) {
  it(`returns to ${destination} after submitting login with real guards`, async () => {
    const router = TestBed.inject(Router);
    const auth = TestBed.inject(AuthService);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(destination, LoginComponent);
    const expectedDestination = destination === '/login' ? '/home' : destination;
    expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe(destination === '/login' ? undefined : destination);
    expect(router.url.split('?')[0]).toBe('/login');
    expect(auth.isAuthenticated()).toBeFalse();

    const loginElement = harness.routeNativeElement!;
    for (const [control, value] of [['identifier', 'unit-user'], ['password', 'fictitious-password']]) {
      const input = loginElement.querySelector<HTMLInputElement>(`input[formControlName="${control}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
    harness.detectChanges();
    loginElement.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const login = http.expectOne('http://localhost:8080/identity/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ username: 'unit-user', email: null, password: 'fictitious-password' });
    const navigation = firstValueFrom(router.events.pipe(filter((event) => event instanceof NavigationEnd)));
    login.flush(testLogin());
    await navigation;
    harness.detectChanges();
    if (destination === '/departments') {
      http.expectOne('http://localhost:8080/department/api/departments').flush([]);
    } else if (destination === '/departments/12/edit') {
      http.expectOne('http://localhost:8080/department/api/departments/12').flush({ id: 12, name: 'Informatique', code: null, description: null });
    }
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(router.url).toBe(expectedDestination);
    expect(auth.isAuthenticated()).toBeTrue();
    const selector = expectedDestination === '/home' ? 'app-home' : expectedDestination.startsWith('/dashboard') ? 'app-dashboard'
      : destination === '/customers/new' ? 'app-customer-form' : destination === '/departments' ? 'app-departments-page' : 'app-department-form';
    expect(harness.routeNativeElement?.querySelector(selector)).not.toBeNull();
    expect(harness.routeNativeElement?.textContent).not.toContain('Connexion CRM');
    http.expectNone('http://localhost:8080/identity/api/auth/refresh');
  });
  }
  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it('lands on home with only authorized links after login for '+role, async () => {
      const router=TestBed.inject(Router);const harness=await RouterTestingHarness.create('/login');
      const component=harness.routeDebugElement!.componentInstance as LoginComponent;
      component['loginForm'].setValue({identifier:'unit-user',password:'fictitious-password'});component['submit']();
      const navigation=firstValueFrom(router.events.pipe(filter(event=>event instanceof NavigationEnd)));
      http.expectOne('http://localhost:8080/identity/api/auth/login').flush({...testLogin(),role,roles:[role]});
      await navigation;harness.detectChanges();await harness.fixture.whenStable();
      expect(router.url).toBe('/home');expect(harness.routeNativeElement?.querySelector('app-home')).not.toBeNull();
      expect(harness.routeNativeElement?.querySelector('a[href="/payroll"]')).toBeNull();
      http.expectNone(request=>request.url.includes('/payroll/')||request.url.includes('/employee/'));
    });
  }
});
