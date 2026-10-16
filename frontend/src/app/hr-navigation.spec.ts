import { dashboardNavigationStub } from './core/testing/dashboard-fixture';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom, of } from 'rxjs';
import { routes } from './app.routes';
import { CrmRole } from './core/models/role.model';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { storeTestSession, testJwt, testLogin } from './core/testing/auth-fixtures';
import { LeaveService } from './features/leaves/leave.service';

describe('HR navigation', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideZonelessChangeDetection(), provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), dashboardNavigationStub,
      { provide: LeaveService, useValue: { list: () => of([]) } }
    ] });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  function session(role: CrmRole): void {
    storeTestSession(testJwt());
    const value = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    value.user.roles = [role];
    sessionStorage.setItem('crm.auth.session', JSON.stringify(value));
  }

  for (const role of [CrmRole.SUPER_ADMIN, CrmRole.ADMIN, CrmRole.EMPLOYEE]) {
    it(`${role} reaches Leave by clicking the HR child without losing the session`, async () => {
      session(role);
      const saved = sessionStorage.getItem('crm.auth.session');
      const harness = await RouterTestingHarness.create('/dashboard');
      const router = TestBed.inject(Router);
      expect(harness.fixture.nativeElement.querySelector('#hr-navigation-title').textContent).toBe('Ressources humaines');
      expect(harness.fixture.nativeElement.querySelector('a[href="/hr"]')).toBeNull();
      const link = Array.from(harness.fixture.nativeElement.querySelectorAll('a'))
        .find((element: any) => element.textContent.includes('Congés')) as HTMLAnchorElement;
      expect(link).toBeTruthy();
      const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
      link.click();
      await navigation;
      await harness.fixture.whenStable();
      expect(router.url).toBe('/leaves');
      expect(sessionStorage.getItem('crm.auth.session')).toBe(saved);
    });
  }

  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it(`enforces ${role} authorization through the HR alias`, async () => {
      session(role);
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl('/hr');
      expect(TestBed.inject(Router).url).toBe(role === CrmRole.EMPLOYEE ? '/leaves' : '/access-denied');
    });
  }

  it('returns an anonymous user to Leave after real login navigation', async () => {
    const harness = await RouterTestingHarness.create('/hr');
    const router = TestBed.inject(Router);
    expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe('/leaves');
    const element = harness.routeNativeElement!;
    for (const [control, value] of [['identifier', 'unit-user'], ['password', 'test-only']]) {
      const input = element.querySelector<HTMLInputElement>(`input[formControlName="${control}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
    harness.detectChanges();
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/identity/api/auth/login')
      .flush({ ...testLogin(), role: CrmRole.ADMIN, roles: [CrmRole.ADMIN] });
    await navigation;
    await harness.fixture.whenStable();
    expect(router.url).toBe('/leaves');
  });

  it('shows the missing-association explanation with the real interceptor and LeaveService', async () => {
    session(CrmRole.EMPLOYEE);
    TestBed.overrideProvider(LeaveService, { useFactory: () => new LeaveService() });
    const harness = await RouterTestingHarness.create('/leaves');
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/hr/api/leaves')
      .flush({ status: 403, error: 'Forbidden', message: 'Employee account is not associated for leave requests', path: '/api/leaves' },
        { status: 403, statusText: 'Forbidden' });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/leaves');
    expect(harness.routeNativeElement?.textContent).toContain('association Employee');
    expect(harness.routeNativeElement?.textContent).toContain('administrateur');
  });

  it('accepts an explicit HR returnUrl after login', async () => {
    const harness = await RouterTestingHarness.create('/login?returnUrl=%2Fhr');
    const element = harness.routeNativeElement!;
    for (const [control, value] of [['identifier', 'unit-user'], ['password', 'test-only']]) {
      const input = element.querySelector<HTMLInputElement>(`input[formControlName="${control}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
    harness.detectChanges();
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const router = TestBed.inject(Router);
    const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
    TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/identity/api/auth/login')
      .flush({ ...testLogin(), role: CrmRole.ADMIN, roles: [CrmRole.ADMIN] });
    await navigation;
    await harness.fixture.whenStable();
    expect(router.url).toBe('/leaves');
  });
});
