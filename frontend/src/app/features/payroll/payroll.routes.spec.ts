import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { routes } from '../../app.routes';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { CrmRole } from '../../core/models/role.model';
import { storeTestSession } from '../../core/testing/auth-fixtures';
import { PayrollFormComponent } from './payroll-form.component';

describe('Payroll routes with real guards and HTTP interceptor', () => {
  const url = 'http://localhost:8080/payroll/api/payrolls';
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); sessionStorage.clear(); TestBed.resetTestingModule(); });
  function session(role: CrmRole) {
    storeTestSession(); const stored = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    stored.user.roles = [role]; sessionStorage.setItem('crm.auth.session', JSON.stringify(stored));
  }
  for (const editing of [false, true]) {
    it(`sends ${editing ? 'PUT' : 'POST'} through Gateway with JWT and DTO fields only`, async () => {
      session(CrmRole.ADMIN);
      const harness = await RouterTestingHarness.create(editing ? '/payroll/4/edit' : '/payroll/new');
      const http = TestBed.inject(HttpTestingController);
      const value = { employeeId: '7', baseSalary: 2000, bonuses: 100, deductions: 50, taxAmount: 200, payPeriod: '2026-10', paymentDate: '2026-10-31', currency: 'TND', status: 'À payer' };
      if (editing) http.expectOne(url + '/4').flush({ id: 4, ...value, netSalary: 123.45 });
      harness.detectChanges();
      const form = harness.fixture.debugElement.query(By.directive(PayrollFormComponent)).componentInstance as PayrollFormComponent;
      form['form'].patchValue(value);
      spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
      form['submit']();
      const write = http.expectOne(editing ? url + '/4' : url);
      expect(write.request.method).toBe(editing ? 'PUT' : 'POST');
      expect(write.request.headers.has('Authorization')).toBeTrue();
      expect(write.request.body).toEqual(value);
      write.flush({ id: 4, ...value, netSalary: 123.45 });
    });
  }
  it('handles a backend forbidden response through the existing interceptor', async () => {
    session(CrmRole.ADMIN); const harness = await RouterTestingHarness.create('/payroll');
    TestBed.inject(HttpTestingController).expectOne(r => r.url === url).flush({}, { status: 403, statusText: 'Forbidden' });
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/access-denied');
  });
  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    for (const path of ['/payroll', '/payroll/new', '/payroll/4', '/payroll/4/edit']) {
      it(`allows ${role} on ${path} and sends reads through Gateway`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        const http = TestBed.inject(HttpTestingController);
        if (path === '/payroll') {
          const request = http.expectOne(r => r.url === url);
          expect(request.request.headers.has('Authorization')).toBeTrue();
          expect(request.request.params.get('sortBy')).toBe('payPeriod');
          request.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
        } else if (path !== '/payroll/new') {
          const request = http.expectOne(url + '/4');
          expect(request.request.headers.has('Authorization')).toBeTrue();
          request.flush({ id: 4, ...{ employeeId: '7', baseSalary: 2000, bonuses: 100, deductions: 50, taxAmount: 200, payPeriod: '2026-10', paymentDate: '2026-10-31', currency: 'TND', status: 'À payer' }, netSalary: 123.45, createdAt: null, updatedAt: null });
        }
        harness.detectChanges(); expect(TestBed.inject(Router).url).toBe(path);
        expect(harness.routeNativeElement?.textContent).not.toContain('Page introuvable');
        expect(harness.fixture.nativeElement.querySelector('a[href="/payroll"]')).toBeTruthy();
      });
    }
  }
  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    for (const path of ['/payroll', '/payroll/new', '/payroll/4', '/payroll/4/edit']) {
      it(`denies ${role} on ${path} before an API call`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        expect(TestBed.inject(Router).url).toBe('/access-denied');
        TestBed.inject(HttpTestingController).expectNone(r => r.url.startsWith(url));
        expect(harness.fixture.nativeElement.querySelector('a[href="/payroll"]')).toBeNull();
      });
    }
  }
  it('redirects an anonymous payroll page to login with its return URL', async () => {
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/payroll');
    const tree = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
    expect(tree.root.children['primary'].segments[0].path).toBe('login');
    expect(tree.queryParams['returnUrl']).toBe('/payroll');
  });
  it('keeps unknown child routes on Page introuvable', async () => {
    session(CrmRole.ADMIN); const harness = await RouterTestingHarness.create('/payroll/4/unknown');
    expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
    expect(TestBed.inject(Router).url).toBe('/payroll/4/unknown');
  });
});
