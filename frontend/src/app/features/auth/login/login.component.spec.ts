import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Observable, Subject, throwError } from 'rxjs';
import { LoginResponse } from '../../../core/models/auth.models';
import { CrmRole } from '../../../core/models/role.model';
import { AuthService } from '../../../core/services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  const loginResponse: LoginResponse = {
    username: 'unit-user',
    email: 'unit@example.test',
    enabled: true,
    token: 'synthetic-access-token',
    refreshToken: 'synthetic-refresh-token',
    type: 'Bearer',
    role: CrmRole.ADMIN,
    roles: [CrmRole.ADMIN],
    expiration: 3600
  };
  let returnUrl: string | null;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    returnUrl = null;
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate', 'navigateByUrl']);
    router.navigate.and.resolveTo(true);
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap({ returnUrl }); } } } }
      ]
    }).compileComponents();
  });

  it('should create the login page', () => {
    const fixture = TestBed.createComponent(LoginComponent);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('validates required fields when the form is empty', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Le nom d'utilisateur ou l'e-mail est obligatoire.");
    expect(fixture.nativeElement.textContent).toContain('Le mot de passe est obligatoire.');
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('validates the password when an identifier is provided', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].controls.identifier.setValue('unit-user');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Le mot de passe est obligatoire.');
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('shows an error and does not navigate when login fails', () => {
    authService.login.and.returnValue(throwError(() => new Error('authentication failed')));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].setValue({
      identifier: 'unit-user',
      password: 'synthetic-password'
    });

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Échec de la connexion');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-access-token');
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-refresh-token');
  });

  it('navigates to home after a successful login', () => {
    authService.login.and.returnValue(new Observable((subscriber) => subscriber.next(loginResponse)));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].setValue({
      identifier: 'unit@example.test',
      password: 'synthetic-password'
    });

    fixture.componentInstance['submit']();

    expect(authService.login).toHaveBeenCalledWith({
      username: null,
      email: 'unit@example.test',
      password: 'synthetic-password'
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });

  it('keeps loading active while the login request is pending', () => {
    const pendingLogin = new Subject<LoginResponse>();
    authService.login.and.returnValue(pendingLogin.asObservable());
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].setValue({
      identifier: 'unit-user',
      password: 'synthetic-password'
    });

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();

    pendingLogin.error(new Error('request failed'));
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBeFalse();
  });

  it('handles an HTTP error without exposing secrets in the template', () => {
    authService.login.and.returnValue(throwError(() => ({ status: 500 })));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].setValue({
      identifier: 'unit-user',
      password: 'synthetic-password'
    });

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-password');
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-access-token');
  });

  for (const [destination, expected] of [
    ['/employees', '/employees'],
    ['/employees?page=2', '/employees?page=2'],
    ['/employees/12#details', '/employees/12#details'],
    ['/employees/12/edit?view=full#form', '/employees/12/edit?view=full#form'],
    ['http://evil.example', '/home'], ['data:text/html,fictitious', '/home'],
    ['\\evil.example', '/home'], ['/%252f%252fevil.example', '/home'],
    ['%2F%2Fevil.example', '/home'], ['%252F%252Fevil.example', '/home'],
    [' /employees', '/home'], ['/employees ', '/home'],
    ['/employees\t', '/home'], ['/employees\n', '/home'],
    ['/employees\u0000', '/home'],
    ['https://example.com', '/home'], ['//example.com', '/home'],
    ['/\\example.com', '/home'], ['/%2f%2fexample.com', '/home'],
    ['/employees/../login', '/home'], ['/login?returnUrl=/login', '/home'],
    ['/unknown', '/home'], ['javascript:alert(1)', '/home'],
    ['/employees(aux:login)', '/home']
  ]) {
    it('handles returnUrl safely: ' + destination, () => {
      returnUrl = destination;
      authService.login.and.returnValue(new Observable((subscriber) => subscriber.next(loginResponse)));
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.componentInstance['loginForm'].setValue({ identifier: 'unit', password: 'fictitious' });
      fixture.componentInstance['submit']();
      expect(router.navigateByUrl).toHaveBeenCalledWith(expected);
    });
  }

  it('does not submit login twice while pending', () => {
    authService.login.and.returnValue(new Subject<LoginResponse>());
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].setValue({ identifier: 'unit', password: 'fictitious' });
    fixture.componentInstance['submit']();
    fixture.componentInstance['submit']();
    expect(authService.login).toHaveBeenCalledTimes(1);
  });

  it('toggles password visibility without submitting the form', () => {
    const fixture = TestBed.createComponent(LoginComponent); fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[formControlName="password"]');
    const toggle: HTMLButtonElement = fixture.nativeElement.querySelector('button[matSuffix]');
    expect(input.type).toBe('password');
    toggle.click(); fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
    expect(toggle.getAttribute('aria-label')).toBe('Masquer le mot de passe');
    toggle.click(); fixture.detectChanges();
    expect(input.type).toBe('password');
    expect(authService.login).not.toHaveBeenCalled();
  });

});
