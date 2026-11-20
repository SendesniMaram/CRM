import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { AuthService } from '../../core/services/auth.service';
import { storeTestSession } from '../../core/testing/auth-fixtures';
import { CrmRole } from '../../core/models/role.model';
import { CrmNavigation } from '../../core/navigation/crm-navigation';
import { safeReturnUrl } from '../../core/services/auth-navigation';
import { roleGuard } from '../../core/guards/role.guard';
import { authGuard } from '../../core/guards/auth.guard';
import { routes } from '../../app.routes';
import { LayoutComponent } from '../../layout/components/layout/layout.component';
import { EmployeeProfileComponent } from './employee-profile.component';

describe('personal profile route and session integration', () => {
  let http: HttpTestingController; let auth: AuthService;
  const url = 'http://localhost:8080/hr/api/employee-profile/me';
  function configure(role: CrmRole) {
    sessionStorage.clear(); storeTestSession(); const value = JSON.parse(sessionStorage.getItem('crm.auth.session')!);
    value.user.roles = [role]; sessionStorage.setItem('crm.auth.session', JSON.stringify(value));
    TestBed.configureTestingModule({ imports: [EmployeeProfileComponent], providers: [provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController); auth = TestBed.inject(AuthService); spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  }
  afterEach(() => { http?.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });
  for (const role of Object.values(CrmRole)) {
    it('preserves guards and exposes the menu only to EMPLOYEE for ' + role, async () => {
      configure(role);
      const route = routes.find(row => row.component === LayoutComponent)!.children!.find(row => row.path === 'profile')!;
      expect(route.canActivate).toEqual([authGuard, roleGuard]); expect(route.data?.['roles']).toEqual(['EMPLOYEE']);
      expect(TestBed.inject(CrmNavigation).items().some(item => item.route === '/profile')).toBe(role === CrmRole.EMPLOYEE);
      expect(safeReturnUrl('/profile')).toBe('/profile');
      const result = await firstValueFrom(TestBed.runInInjectionContext(() => roleGuard({ data: route.data } as ActivatedRouteSnapshot,
        { url: '/profile' } as RouterStateSnapshot)) as Observable<any>);
      expect(result === true).toBe(role === CrmRole.EMPLOYEE);
    });
  }
  it('preserves the session on direct HR 503', () => {
    configure(CrmRole.EMPLOYEE); const fixture = TestBed.createComponent(EmployeeProfileComponent); fixture.detectChanges();
    const token = auth.getToken(); const request = http.expectOne(url);
    expect(request.request.headers.get('Authorization')).toBe('Bearer ' + token);
    request.flush({}, { status: 503, statusText: 'Unavailable' }); fixture.detectChanges();
    expect(auth.getToken()).toBe(token); http.expectNone(row => row.url.includes('/api/auth/refresh') || row.url.includes('/api/internal/'));
    expect(fixture.nativeElement.textContent).toContain('temporairement indisponible');
  });
  it('loads the own department label through the guarded directory endpoint', () => {
    configure(CrmRole.EMPLOYEE); const fixture = TestBed.createComponent(EmployeeProfileComponent); fixture.detectChanges();
    const token = auth.getToken(); const profile = http.expectOne(url);
    expect(profile.request.headers.get('Authorization')).toBe('Bearer ' + token);
    profile.flush({ employeeId: '103', firstName: 'Test', lastName: 'Employee', jobTitle: 'Developer', departmentId: '7', hireDate: '2026-01-15' });
    const department = http.expectOne('http://localhost:8080/department/api/departments/7');
    expect(department.request.headers.get('Authorization')).toBe('Bearer ' + token);
    department.flush({ id: 7, name: 'Engineering' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Engineering');
  });
  it('does not discard session or replay a profile request when refresh is unavailable', () => {
    configure(CrmRole.EMPLOYEE); const fixture = TestBed.createComponent(EmployeeProfileComponent); fixture.detectChanges(); const token = auth.getToken();
    http.expectOne(url).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('http://localhost:8080/identity/api/auth/refresh').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges(); expect(auth.getToken()).toBe(token); http.expectNone(url);
    expect((fixture.componentInstance as any).profile()).toBeNull();
  });
});
