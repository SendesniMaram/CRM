import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EmployeeRequest, EmployeeResponse } from '../models/employee.models';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  const employeesUrl = 'http://localhost:8080/employee/api/employees';
  const employee: EmployeeResponse = {
    id: 1,
    employeeCode: 'EMP001',
    firstName: 'John',
    lastName: 'Doe',
    salary: 60000
  };
  const request: EmployeeRequest = {
    employeeCode: 'EMP001',
    firstName: 'John',
    lastName: 'Doe',
    salary: 60000
  };
  let service: EmployeeService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        EmployeeService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideZonelessChangeDetection()
      ]
    });
    service = TestBed.inject(EmployeeService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('gets the unpaged employee list', () => {
    service.getEmployees().subscribe((response) => expect(response).toEqual([employee]));

    const request = httpTesting.expectOne(employeesUrl);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([employee]);
  });

  it('gets a paginated employee list', () => {
    service.getEmployeesPage({ page: 1, size: 20 }).subscribe();

    const request = httpTesting.expectOne((httpRequest) => httpRequest.url === employeesUrl);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    request.flush({ content: [employee], totalElements: 1, totalPages: 1, size: 20, number: 1 });
  });

  it('gets employees with a keyword', () => {
    service.getEmployeesPage({ page: 0, size: 10, keyword: 'John' }).subscribe();

    const request = httpTesting.expectOne((httpRequest) => httpRequest.url === employeesUrl);
    expect(request.request.params.get('keyword')).toBe('John');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [employee], totalElements: 1, totalPages: 1, size: 10, number: 0 });
  });

  it('gets employees with sorting parameters', () => {
    service.getEmployeesPage({ sortBy: 'lastName', direction: 'desc' }).subscribe();

    const request = httpTesting.expectOne((httpRequest) => httpRequest.url === employeesUrl);
    expect(request.request.params.get('sortBy')).toBe('lastName');
    expect(request.request.params.get('direction')).toBe('desc');
    request.flush({ content: [employee], totalElements: 1, totalPages: 1, size: 10, number: 0 });
  });

  it('gets an employee by id', () => {
    service.getEmployeeById(1).subscribe((response) => expect(response).toEqual(employee));

    const request = httpTesting.expectOne(`${employeesUrl}/1`);
    expect(request.request.method).toBe('GET');
    request.flush(employee);
  });

  it('gets employee details', () => {
    service.getEmployeeDetails(1).subscribe();

    const request = httpTesting.expectOne(`${employeesUrl}/1/details`);
    expect(request.request.method).toBe('GET');
    request.flush({ employee, department: { id: 2, name: 'Engineering', description: '', code: 'ENG', createdAt: '', updatedAt: '' } });
  });

  it('creates an employee', () => {
    service.createEmployee(request).subscribe((response) => expect(response).toEqual(employee));

    const httpRequest = httpTesting.expectOne(employeesUrl);
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(employee, { status: 201, statusText: 'Created' });
  });

  it('updates an employee', () => {
    service.updateEmployee(1, request).subscribe((response) => expect(response).toEqual(employee));

    const httpRequest = httpTesting.expectOne(`${employeesUrl}/1`);
    expect(httpRequest.request.method).toBe('PUT');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(employee);
  });

  it('deletes an employee', () => {
    service.deleteEmployee(1).subscribe();

    const request = httpTesting.expectOne(`${employeesUrl}/1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
