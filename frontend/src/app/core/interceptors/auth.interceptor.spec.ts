import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getToken']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideZonelessChangeDetection()
      ]
    });
    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('adds a bearer authorization header when a token exists', () => {
    authService.getToken.and.returnValue('synthetic-access-token');

    httpClient.get('/protected').subscribe();
    const request = httpTesting.expectOne('/protected');

    expect(request.request.headers.get('Authorization')).toBe('Bearer synthetic-access-token');
    request.flush({});
  });

  it('does not add authorization when there is no token', () => {
    authService.getToken.and.returnValue(null);

    httpClient.get('/public').subscribe();
    const request = httpTesting.expectOne('/public');

    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('preserves other request headers', () => {
    authService.getToken.and.returnValue('synthetic-access-token');

    httpClient.get('/protected', { headers: { 'X-Test-Header': 'kept' } }).subscribe();
    const request = httpTesting.expectOne('/protected');

    expect(request.request.headers.get('X-Test-Header')).toBe('kept');
    request.flush({});
  });

  it('does not log the token', () => {
    const consoleSpy = spyOn(console, 'log');
    authService.getToken.and.returnValue('synthetic-access-token');

    httpClient.get('/protected').subscribe();
    const request = httpTesting.expectOne('/protected');
    request.flush({});

    expect(consoleSpy).not.toHaveBeenCalled();
  });
});