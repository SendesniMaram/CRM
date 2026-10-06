import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { DepartmentResponse } from '../../employees/models/employee.models';
import { DepartmentService } from './department.service';

describe('DepartmentService', () => {
  let service: DepartmentService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/department/api/departments';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DepartmentService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('gets the complete array from the exact Gateway URL without query parameters', () => {
    const departments: DepartmentResponse[] = [
      { id: 5, name: 'Finance', code: 'FIN', description: '', createdAt: '', updatedAt: '' }
    ];
    let response: DepartmentResponse[] | undefined;
    service.getDepartments().subscribe((value) => response = value);
    const request = http.expectOne(url);
    expect(request.request.method).toBe('GET');
    expect(request.request.urlWithParams).toBe(url);
    expect(request.request.params.keys()).toEqual([]);
    request.flush(departments);
    expect(response).toEqual(departments);
  });

  it('propagates HTTP errors', () => {
    let failure: HttpErrorResponse | undefined;
    service.getDepartments().subscribe({ next: () => fail('Expected an error'), error: (error: HttpErrorResponse) => failure = error });
    http.expectOne(url).flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
    expect(failure?.status).toBe(503);
  });
});
