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

describe('Customers routes with real guards and HTTP interceptor', () => {
  const url = 'http://localhost:8080/customer/api/customers';
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
  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    for (const path of ['/customers', '/customers/new', '/customers/4', '/customers/4/edit']) {
      it(`allows ${role} on ${path} and sends reads through Gateway`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        const http = TestBed.inject(HttpTestingController);
        if (path === '/customers') {
          const request = http.expectOne(r => r.url === url);
          expect(request.request.headers.has('Authorization')).toBeTrue();
          expect(request.request.params.get('sortBy')).toBe('name');
          request.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
        } else if (path !== '/customers/new') {
          const request = http.expectOne(url + '/4');
          expect(request.request.headers.has('Authorization')).toBeTrue();
          request.flush({ id: 4, name: 'Client test', email: null, phone: null, company: null, address: null, createdAt: null, updatedAt: null });
          if (path === '/customers/4') {
            const interactions = http.expectOne(url + '/4/interactions');
            expect(interactions.request.headers.has('Authorization')).toBeTrue();
            interactions.flush([]);
          }
        }
        harness.detectChanges(); expect(TestBed.inject(Router).url).toBe(path);
        expect(harness.routeNativeElement?.textContent).not.toContain('Page introuvable');
        expect(harness.fixture.nativeElement.querySelector('a[href="/customers"]')).toBeTruthy();
      });
    }
  }
  for (const role of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    for (const path of ['/customers', '/customers/new', '/customers/4', '/customers/4/edit']) {
      it(`denies ${role} on ${path} before an API call`, async () => {
        session(role); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
        expect(TestBed.inject(Router).url).toBe('/access-denied');
        TestBed.inject(HttpTestingController).expectNone(r => r.url.startsWith(url));
        expect(harness.fixture.nativeElement.querySelector('a[href="/customers"]')).toBeNull();
      });
    }
  }
  it('redirects an anonymous client page to login with its return URL', async () => {
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/customers');
    const tree = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
    expect(tree.root.children['primary'].segments[0].path).toBe('login');
    expect(tree.queryParams['returnUrl']).toBe('/customers');
  });
  it('loads the commercial pipeline and client directory through Gateway for admins', async () => {
    session(CrmRole.ADMIN);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/customers/opportunities');
    const http = TestBed.inject(HttpTestingController);
    const opportunities = http.expectOne(r => r.url === 'http://localhost:8080/customer/api/opportunities');
    expect(opportunities.request.headers.has('Authorization')).toBeTrue();
    opportunities.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
    const contacts = http.expectOne(r => r.url === url);
    expect(contacts.request.headers.has('Authorization')).toBeTrue();
    contacts.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
    expect(TestBed.inject(Router).url).toBe('/customers/opportunities');
    expect(harness.fixture.nativeElement.textContent).toContain('Pipeline commercial');
  });
  it('denies the commercial pipeline to clients before any API call', async () => {
    session(CrmRole.CLIENT);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/customers/opportunities');
    expect(TestBed.inject(Router).url).toBe('/access-denied');
    TestBed.inject(HttpTestingController).expectNone(r => r.url.includes('/api/opportunities'));
  });
  it('keeps unknown child routes on Page introuvable', async () => {
    session(CrmRole.ADMIN); const harness = await RouterTestingHarness.create('/customers/4/unknown');
    expect(harness.routeNativeElement?.textContent).toContain('Page introuvable');
    expect(TestBed.inject(Router).url).toBe('/customers/4/unknown');
  });
});
