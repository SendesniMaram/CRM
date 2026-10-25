import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { CrmRole } from '../../core/models/role.model';
import { storeTestSession } from '../../core/testing/auth-fixtures';
describe('Invoices routes with real guards and interceptor', () => {
  const invoices = 'http://localhost:8080/invoice/api/invoices';
  const customers = 'http://localhost:8080/customer/api/customers';
  beforeEach(() => {
    sessionStorage.clear(); TestBed.configureTestingModule({ providers: [provideRouter(routes),
      provideZonelessChangeDetection(), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); sessionStorage.clear(); TestBed.resetTestingModule(); });
  function session(role: CrmRole) {
    storeTestSession(); const stored = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    stored.user.roles = [role]; sessionStorage.setItem('crm.auth.session', JSON.stringify(stored));
  }
  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    for (const path of ['/invoices', '/invoices/new', '/invoices/4', '/invoices/4/edit']) {
      it(`allows ${role} on ${path} and calls only Gateway`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        const http = TestBed.inject(HttpTestingController);
        const form = path.endsWith('/new') || path.endsWith('/edit');
        if (form) {
          const req = http.expectOne(r => r.url === customers);
          expect(req.request.headers.has('Authorization')).toBeTrue();
          req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 25 });
        }
        if (path === '/invoices') {
          const req = http.expectOne(r => r.url === invoices);
          expect(req.request.headers.has('Authorization')).toBeTrue();
          expect(req.request.params.get('sortBy')).toBe('invoiceNumber');
          req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
        } else if (path !== '/invoices/new') {
          const req = http.expectOne(invoices + '/4');
          expect(req.request.headers.has('Authorization')).toBeTrue();
          req.flush({ id: 4, invoiceNumber: 'FACT-TEST', customerId: '2', issueDate: null, dueDate: null,
            subtotal: 100, tax: null, totalAmount: 100, currency: null, status: 'À traiter', notes: null, createdAt: null, updatedAt: null });
          if (form) {
            const client = http.expectOne(customers + '/2');
            expect(client.request.headers.has('Authorization')).toBeTrue();
            client.flush({ id: 2, name: 'Client test', company: null, email: null, phone: null, address: null, createdAt: null, updatedAt: null });
          }
        }
        harness.detectChanges(); expect(TestBed.inject(Router).url).toBe(path);
        expect(harness.fixture.nativeElement.querySelector('a[href="/invoices"]')).toBeTruthy();
      });
    }
  }
  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    for (const path of ['/invoices', '/invoices/new', '/invoices/4', '/invoices/4/edit']) {
      it(`denies ${role} on ${path} before calling either service`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        expect(TestBed.inject(Router).url).toBe('/access-denied');
        expect(harness.fixture.nativeElement.querySelector('a[href="/invoices"]')).toBeNull();
        TestBed.inject(HttpTestingController).expectNone(r => r.url.startsWith(invoices) || r.url.startsWith(customers));
      });
    }
  }
  it('redirects an anonymous invoice route to login', async () => {
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/invoices');
    const tree = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
    expect(tree.root.children['primary'].segments[0].path).toBe('login');
    expect(tree.queryParams['returnUrl']).toBe('/invoices');
  });
  it('preserves unknown invoice paths as Page introuvable', async () => {
    session(CrmRole.ADMIN); const harness = await RouterTestingHarness.create('/invoices/4/unknown');
    expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
    expect(TestBed.inject(Router).url).toBe('/invoices/4/unknown');
  });
});
