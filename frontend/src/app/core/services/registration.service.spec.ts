import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RegisterRequest } from '../models/register.models';
import { RegistrationService } from './registration.service';

describe('RegistrationService', () => {
  const registerUrl = 'http://localhost:8080/identity/api/auth/register';
  const request: RegisterRequest = {
    firstName: 'Ada',
    lastName: 'Lovelace',
    username: 'ada-user',
    email: 'ada@example.test',
    password: 'synthetic-password'
  };
  let service: RegistrationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RegistrationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideZonelessChangeDetection()
      ]
    });
    service = TestBed.inject(RegistrationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('posts the exact registration payload to the identity gateway', () => {
    service.register(request).subscribe();

    const httpRequest = httpTesting.expectOne(registerUrl);
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(null, { status: 201, statusText: 'Created' });
  });
});