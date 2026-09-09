import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { LoginRequest, LoginResponse } from '../models/auth.models';

describe('AuthService', () => {
  const loginUrl = 'http://localhost:8080/identity/api/auth/login';
  const request: LoginRequest = {
    username: 'unit-user',
    email: null,
    password: 'synthetic-password'
  };
  const response: LoginResponse = {
    username: 'unit-user',
    email: 'unit@example.test',
    enabled: true,
    token: 'synthetic-access-token',
    refreshToken: 'synthetic-refresh-token',
    type: 'Bearer',
    role: 'USER',
    roles: ['USER'],
    expiration: 3600
  };
  const sessionKey = 'crm.auth.session';
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideZonelessChangeDetection()
      ]
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('posts the login request to the identity API with the expected payload', () => {
    let actualResponse: LoginResponse | undefined;
    service.login(request).subscribe((value) => (actualResponse = value));

    const httpRequest = httpTesting.expectOne(loginUrl);
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(response);

    expect(actualResponse).toEqual(response);
  });

  it('stores the authenticated session and exposes its user data', () => {
    service.login(request).subscribe();
    httpTesting.expectOne(loginUrl).flush(response);

    expect(JSON.parse(sessionStorage.getItem(sessionKey) ?? '{}')).toEqual({
      accessToken: response.token,
      refreshToken: response.refreshToken,
      user: {
        username: response.username,
        email: response.email,
        enabled: response.enabled,
        type: response.type,
        role: response.role,
        roles: response.roles,
        expiration: response.expiration
      }
    });
    expect(service.getToken()).toBe(response.token);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.getCurrentUser()).toEqual(jasmine.objectContaining({
      username: response.username,
      email: response.email,
      role: response.role
    }));
  });

  it('removes the session on logout', () => {
    sessionStorage.setItem(sessionKey, JSON.stringify({ accessToken: 'synthetic-token' }));

    service.logout();

    expect(sessionStorage.getItem(sessionKey)).toBeNull();
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('treats malformed stored session data as unauthenticated', () => {
    sessionStorage.setItem(sessionKey, '{invalid-json');

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem(sessionKey)).toBeNull();
  });

  it('does not log authentication secrets', () => {
    const consoleSpy = spyOn(console, 'log');

    service.login(request).subscribe();
    httpTesting.expectOne(loginUrl).flush(response);

    expect(consoleSpy).not.toHaveBeenCalled();
  });
});