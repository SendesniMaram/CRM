import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { AuthService } from '../../core/services/auth.service';
import { storeTestSession } from '../../core/testing/auth-fixtures';
import { UsersPageComponent } from './users-page.component';
import { EmployeeAssociationComponent } from './employee-association.component';

describe('onboarding with real HTTP services, interceptor and session', () => {
  let http: HttpTestingController; let auth: AuthService; let router: Router;
  const users = 'http://localhost:8080/identity/api/users';
  const leaves = 'http://localhost:8080/hr/api/leaves';
  const refresh = 'http://localhost:8080/identity/api/auth/refresh';
  const body = { firstName: 'Test', lastName: 'Employee', username: 'created-user', email: 'created@example.test', password: 'test-only-password', phone: '' };
  beforeEach(() => {
    sessionStorage.clear(); storeTestSession();
    TestBed.configureTestingModule({ imports: [UsersPageComponent, EmployeeAssociationComponent], providers: [
      provideZonelessChangeDetection(), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])
    ] });
    http = TestBed.inject(HttpTestingController); auth = TestBed.inject(AuthService); router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });

  for (const status of [400, 401, 403, 404, 409, 502, 503]) {
    it('preserves confirmed account creation when association fails with ' + status, async () => {
      const accountFixture = TestBed.createComponent(UsersPageComponent); accountFixture.detectChanges();
      const account = accountFixture.componentInstance as any; http.expectOne(users).flush('[]');
      account.form.setValue(body); account.create();
      const creation = http.expectOne(users); expect(creation.request.headers.get('Authorization')).toBe('Bearer ' + auth.getToken());
      expect(creation.request.method).toBe('POST'); creation.flush(null, { status: 201, statusText: 'Created' });
      await accountFixture.whenStable(); http.expectOne(users).flush('[]');
      expect(account.createdUsername()).toBe(body.username);
      const fixture = TestBed.createComponent(EmployeeAssociationComponent); fixture.detectChanges();
      const page = fixture.componentInstance as any; page.load();
      const options = { users: [{ id: 42, label: body.username }], employees: [{ id: 103, label: 'E2E-103' }] };
      http.expectOne(leaves + '/associations/options').flush(JSON.stringify(options));
      page.user.setValue('42'); page.select();
      http.expectOne('http://localhost:8080/hr/api/employee-associations/users/42').flush({}, { status: 404, statusText: 'Not Found' });
      page.employee.setValue('103'); page.create();
      const association = http.expectOne('http://localhost:8080/hr/api/employee-associations');
      expect(association.request.body).toEqual({ userId: '42', employeeId: '103' });
      expect(association.request.headers.get('Authorization')).toBe('Bearer ' + auth.getToken());
      const token = auth.getToken(); association.flush({}, { status, statusText: 'Synthetic refusal' });
      if (status === 401) http.expectOne(refresh).flush({}, { status: 503, statusText: 'Unavailable' });
      else http.expectNone(refresh);
      expect(page.notice()).toBe(''); expect(page.user.value).toBe('42'); expect(page.employee.value).toBe('103');
      expect(account.createdUsername()).toBe(body.username); expect(account.form.controls.password.value).toBe('');
      expect(auth.getToken()).toBe(token);
      http.expectNone(request => ['DELETE', 'PUT'].includes(request.method));
      http.expectNone(request => request.method === 'POST' && request.url === users);
      http.expectNone('http://localhost:8080/hr/api/employee-associations/me');
      if (status !== 403) expect(router.navigate).not.toHaveBeenCalled();
      if (status === 503) {
        page.read(); http.expectOne('http://localhost:8080/hr/api/employee-associations/users/42')
          .flush({}, { status: 404, statusText: 'Not Found' });
        page.create(); const retry = http.expectOne('http://localhost:8080/hr/api/employee-associations');
        expect(retry.request.body).toEqual({ userId: '42', employeeId: '103' });
        retry.flush({ userId: '42', employeeId: '103' }, { status: 201, statusText: 'Created' });
        await fixture.whenStable(); http.expectOne('http://localhost:8080/hr/api/employee-associations/users/42')
          .flush({ userId: '42', employeeId: '103' });
        expect(page.notice()).toContain('Association'); expect(account.createdUsername()).toBe(body.username);
      }
    });
  }
});
