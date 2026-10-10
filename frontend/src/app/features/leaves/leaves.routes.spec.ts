import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom, of } from 'rxjs';
import { routes } from '../../app.routes';
import { CrmRole } from '../../core/models/role.model';
import { safeReturnUrl } from '../../core/services/auth-navigation';
import { storeTestSession, testJwt, testLogin } from '../../core/testing/auth-fixtures';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { LeaveService } from './leave.service';
describe('Leave routing', () => {
  beforeEach(() => {
    sessionStorage.clear(); TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter(routes), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: LeaveService, useValue: { list: () => of([]) } }]
    });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); sessionStorage.clear(); TestBed.resetTestingModule(); });
  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN, CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it(`enforces ${role} access to the list`, async () => {
      storeTestSession(testJwt()); const session = JSON.parse(sessionStorage.getItem('crm.auth.session')!); session.user.roles = [role]; sessionStorage.setItem('crm.auth.session', JSON.stringify(session));
      const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/leaves'); expect(TestBed.inject(Router).url).toBe(role === CrmRole.CLIENT ? '/access-denied' : '/leaves');
    });
  }
  for (const role of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN, CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it(`enforces ${role} access to personal creation`, async () => {
      storeTestSession(testJwt()); const session = JSON.parse(sessionStorage.getItem('crm.auth.session')!); session.user.roles = [role]; sessionStorage.setItem('crm.auth.session', JSON.stringify(session));
      const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/leaves/new'); expect(TestBed.inject(Router).url).toBe(role === CrmRole.EMPLOYEE ? '/leaves/new' : '/access-denied');
    });
  }
  for (const path of ['/leaves', '/leaves/new']) {
    it(`returns an EMPLOYEE to ${path} after submitting login`, async () => {
      const router = TestBed.inject(Router);
      const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path);
      const element = harness.routeNativeElement!;
      for (const [control, value] of [['identifier', 'unit-user'], ['password', 'test-only']]) {
        const input = element.querySelector<HTMLInputElement>(`input[formControlName="${control}"]`)!;
        input.value = value; input.dispatchEvent(new Event('input', { bubbles: true }));
      }
      harness.detectChanges(); element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
      TestBed.inject(HttpTestingController).expectOne('http://localhost:8080/identity/api/auth/login')
        .flush({ ...testLogin(), role: CrmRole.EMPLOYEE, roles: [CrmRole.EMPLOYEE] });
      await navigation; await harness.fixture.whenStable(); harness.detectChanges();
      expect(router.url).toBe(path); expect(harness.routeNativeElement?.textContent).not.toContain('Connexion CRM');
    });
    it(`preserves login returnUrl ${path}`, async () => {
      const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(path); const tree = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
      expect(tree.queryParams['returnUrl']).toBe(path); expect(safeReturnUrl(tree.queryParams['returnUrl'])).toBe(path);
    });
  }
});
