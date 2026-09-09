import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
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

    expect(fixture.nativeElement.textContent).toContain('Username or email is required.');
    expect(fixture.nativeElement.textContent).toContain('Password is required.');
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('validates the password when an identifier is provided', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance['loginForm'].controls.identifier.setValue('unit-user');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Password is required.');
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

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Login failed');
    expect(router.navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-access-token');
    expect(fixture.nativeElement.textContent).not.toContain('synthetic-refresh-token');
  });

  it('navigates to the dashboard after a successful login', () => {
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
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
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
    expect(fixture.nativeElement.querySelector('button').disabled).toBeTrue();

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
});
