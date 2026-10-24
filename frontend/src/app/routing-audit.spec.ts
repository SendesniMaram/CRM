import { dashboardNavigationStub } from './core/testing/dashboard-fixture';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { CrmRole } from './core/models/role.model';
import { AuthService } from './core/services/auth.service';
import { storeTestSession, testJwt } from './core/testing/auth-fixtures';
import { DepartmentService } from './features/departments/services/department.service';
import { EmployeeService } from './features/employees/services/employee.service';

describe('application routing audit with restored sessions', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideZonelessChangeDetection(), provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), dashboardNavigationStub,
      { provide: DepartmentService, useValue: {
        getDepartments: () => of([]),
        getDepartmentById: () => of({ id: 12, name: 'Informatique', code: null, description: null })
      } },
      { provide: EmployeeService, useValue: { getEmployeesPage: () => of({ content: [], totalElements: 0 }) } }
    ] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });

  for (const role of [CrmRole.SUPER_ADMIN, CrmRole.ADMIN, CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    for (const destination of ['/dashboard', '/employees', '/departments', '/departments/new']) {
      it(`restores ${role} on ${destination} and enforces route roles`, async () => {
        storeTestSession(testJwt());
        const session = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
        session.user.roles = [role];
        sessionStorage.setItem('crm.auth.session', JSON.stringify(session));
        const harness = await RouterTestingHarness.create();
        await harness.navigateByUrl(destination);
        if (role === CrmRole.CLIENT && destination === '/dashboard') {
          const http = TestBed.inject(HttpTestingController);
          http.expectOne('http://localhost:8080/customer/api/customers/me')
            .flush({customerId:'101',name:'Client',email:null,phone:null,company:null});
          http.expectOne(r => r.url === 'http://localhost:8080/invoice/api/invoices/me')
            .flush({content:[],totalElements:0,totalPages:0,number:0});
          harness.detectChanges();
        }
        const allowed = destination === '/dashboard' || role === CrmRole.SUPER_ADMIN || role === CrmRole.ADMIN;
        expect(TestBed.inject(Router).url).toBe(allowed ? destination : '/access-denied');
        expect(TestBed.inject(AuthService).isAuthenticated()).toBeTrue();
      });
    }
  }

  for (const destination of ['/dashboard', '/employees', '/departments', '/departments/new']) {
    it(`redirects a session without renewal credentials from ${destination} to login`, async () => {
      const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(destination);
      const tree = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
      expect(tree.root.children['primary'].segments.map((segment) => segment.path)).toEqual(['login']);
      expect(tree.queryParams['returnUrl']).toBe(destination);
    });
  }

  it('routes new to creation and a numeric ID to a prefilled edit form', async () => {
    storeTestSession(); const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/departments/new');
    expect(harness.routeNativeElement?.textContent).toContain('Nouveau département');
    expect(harness.routeNativeElement?.querySelector<HTMLInputElement>('input[formControlName="name"]')?.value).toBe('');
    await harness.navigateByUrl('/departments/12/edit');
    expect(harness.routeNativeElement?.textContent).toContain('Modifier le département');
    expect(harness.routeNativeElement?.querySelector<HTMLInputElement>('input[formControlName="name"]')?.value).toBe('Informatique');
  });

  it('redirects root to home and preserves unknown paths as not found without losing the session', async () => {
    storeTestSession(); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/home');
    for (const path of ['/route-qui-nexiste-pas', '/departments/12/unknown']) {
      await harness.navigateByUrl(path); expect(TestBed.inject(Router).url).toBe(path);
      expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
      expect(TestBed.inject(AuthService).isAuthenticated()).toBeTrue();
    }
  });

  for (const path of ['/login', '/register']) {
    it(`keeps ${path} public even with a valid session, matching the current configuration`, async () => {
      storeTestSession(); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
      expect(TestBed.inject(Router).url).toBe(path);
      expect(TestBed.inject(AuthService).isAuthenticated()).toBeTrue();
    });
  }
});
