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

describe('Login returnUrl with application routes and real AuthService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideZonelessChangeDetection(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting()
    ] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    sessionStorage.clear();
  });

  it('returns to the protected destination including query and fragment after submitting login', async () => {
    const destination = '/dashboard?view=summary#stats';
    const router = TestBed.inject(Router);
    const auth = TestBed.inject(AuthService);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(destination, LoginComponent);
    expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe(destination);
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
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(router.url).toBe(destination);
    expect(auth.isAuthenticated()).toBeTrue();
    expect(harness.routeNativeElement?.querySelector('app-dashboard')).not.toBeNull();
    expect(harness.routeNativeElement?.querySelector('app-login')).toBeNull();
    http.expectNone('http://localhost:8080/identity/api/auth/refresh');
  });
});
