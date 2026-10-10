import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withInMemoryScrolling } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { CrmRole } from './core/models/role.model';
import { storeTestSession, testJwt } from './core/testing/auth-fixtures';
import { LeaveService } from './features/leaves/leave.service';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';

describe('available CRM navigation', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideZonelessChangeDetection(), provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' })),
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
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
    const stored = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    stored.user.roles = [role];
    sessionStorage.setItem('crm.auth.session', JSON.stringify(stored));
  }

  for (const role of Object.values(CrmRole)) {
    it(`keeps unknown and unimplemented URLs out of Login for ${role}`, async () => {
      session(role);
      const saved = sessionStorage.getItem('crm.auth.session');
      const harness = await RouterTestingHarness.create();
      for (const path of ['/unknown', '/customers', '/payroll', '/fees', '/invoices', '/employee-area', '/client-area']) {
        await harness.navigateByUrl(path);
        expect(TestBed.inject(Router).url).toBe(path);
        expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
        expect(harness.routeNativeElement?.textContent).not.toContain('Connexion CRM');
      }
      expect(sessionStorage.getItem('crm.auth.session')).toBe(saved);
    });

    it(`restores ${role} permissions after loading the app from session storage`, async () => {
      session(role);
      const harness = await RouterTestingHarness.create('/dashboard');
      expect(TestBed.inject(Router).url).toBe('/dashboard');
      const text = harness.fixture.nativeElement.textContent;
      if (role === CrmRole.CLIENT) expect(text).not.toContain('Congés');
      else expect(text).toContain('Congés');
    });
  }

  it('shows a navigation error for an anonymous unknown URL without inventing a login destination', async () => {
    const harness = await RouterTestingHarness.create('/unknown');
    expect(TestBed.inject(Router).url).toBe('/unknown');
    expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
    await harness.navigateByUrl('/dashboard');
    expect(TestBed.inject(Router).parseUrl(TestBed.inject(Router).url).queryParams['returnUrl']).toBe('/dashboard');
  });

  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it(`denies ${role} on existing administrative pages without Login`, async () => {
      session(role);
      const harness = await RouterTestingHarness.create();
      for (const path of ['/employees', '/departments']) {
        await harness.navigateByUrl(path);
        expect(TestBed.inject(Router).url).toBe('/access-denied');
      }
    });
  }

  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it(`lets ${role} reach existing Employee and Department management`, async () => {
      session(role);
      const harness = await RouterTestingHarness.create('/employees');
      const http = TestBed.inject(HttpTestingController);
      http.expectOne(request => request.url === 'http://localhost:8080/employee/api/employees')
        .flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
      await harness.fixture.whenStable();
      expect(TestBed.inject(Router).url).toBe('/employees');
      await harness.navigateByUrl('/departments');
      http.expectOne('http://localhost:8080/department/api/departments').flush([]);
      await harness.fixture.whenStable();
      expect(TestBed.inject(Router).url).toBe('/departments');
    });

    it(`offers ${role} an administrative anchor and loads existing association options`, async () => {
      session(role);
      TestBed.overrideProvider(LeaveService, { useFactory: () => new LeaveService() });
      const harness = await RouterTestingHarness.create('/leaves#associations');
      const http = TestBed.inject(HttpTestingController);
      http.expectOne('http://localhost:8080/hr/api/leaves').flush([]);
      await harness.fixture.whenStable();
      harness.detectChanges();
      expect(harness.routeNativeElement?.querySelector('#associations')).toBeTruthy();
      expect(harness.fixture.nativeElement.querySelector('a[href="/leaves#associations"]')).toBeTruthy();
      const buttons = Array.from(harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('button'));
      buttons.find(button => button.textContent?.includes('Charger les comptes et employés'))!.click();
      http.expectOne('http://localhost:8080/hr/api/leaves/associations/options').flush({
        users: [{ id: 2, label: 'leave-employee-a' }, { id: 3, label: 'leave-employee-b' }],
        employees: [{ id: 1, label: 'EMP-TEST-01' }, { id: 2, label: 'EMP-TEST-02' }]
      });
      http.expectOne('http://localhost:8080/hr/api/leaves/associations').flush([]);
      await harness.fixture.whenStable();
      harness.detectChanges();
      expect(harness.routeNativeElement?.querySelectorAll('mat-select').length).toBe(2);
      expect(harness.routeNativeElement?.textContent).toContain('Associer');
      const selects = await TestbedHarnessEnvironment.loader(harness.fixture).getAllHarnesses(MatSelectHarness);
      await selects[0].open();
      expect(await Promise.all((await selects[0].getOptions()).map(option => option.getText())))
        .toEqual(['leave-employee-a', 'leave-employee-b']);
      await selects[0].clickOptions({ text: 'leave-employee-a' });
      await selects[1].open();
      expect(await Promise.all((await selects[1].getOptions()).map(option => option.getText())))
        .toEqual(['EMP-TEST-01', 'EMP-TEST-02']);
      await selects[1].clickOptions({ text: 'EMP-TEST-02' });
      harness.detectChanges();
      expect(harness.routeNativeElement?.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBeFalse();
      http.expectNone(request => request.method === 'POST');
    });
  }
});
