import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeProfileService } from './employee-profile.service';

describe('EmployeeProfileService', () => {
  let service: EmployeeProfileService; let http: HttpTestingController;
  const url = 'http://localhost:8080/hr/api/employee-profile/me';
  const profile = { employeeId: '9223372036854775807', firstName: 'Test', lastName: 'Employee', jobTitle: 'Developer', departmentId: '9007199254740993', hireDate: '2026-01-15' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EmployeeProfileService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('uses only the fixed personal endpoint without target IDs and preserves long strings', () => {
    service.read().subscribe(value => expect(value).toEqual(profile));
    const request = http.expectOne(url); expect(request.request.method).toBe('GET'); expect(request.request.params.keys()).toEqual([]);
    expect(request.request.body).toBeNull(); expect(request.request.headers.keys()).toEqual([]); request.flush(profile);
  });
  it('preserves nullable professional fields', () => {
    const nullable = { employeeId: '103', firstName: null, lastName: null, jobTitle: null, departmentId: null, hireDate: null };
    service.read().subscribe(value => expect(value).toEqual(nullable)); http.expectOne(url).flush(nullable);
  });
  for (const value of [null, {}, { ...profile, employeeId: 103 }, { ...profile, employeeId: '01' },
    { ...profile, employeeId: '9223372036854775808' }, { ...profile, departmentId: 7 }, { ...profile, hireDate: '2026-02-30' },
    { ...profile, firstName: {} }, { ...profile, salary: 1234 }]) {
    it('rejects an invalid or excessive profile contract ' + JSON.stringify(value), () => {
      service.read().subscribe({ next: () => fail('Unexpected success'), error: error => expect(error.status).toBe(502) }); http.expectOne(url).flush(value);
    });
  }
  for (const status of [401, 403, 404, 502, 503]) {
    it('preserves HTTP classification ' + status, () => {
      service.read().subscribe({ next: () => fail('Unexpected success'), error: error => expect(error.status).toBe(status) });
      http.expectOne(url).flush({}, { status, statusText: 'Synthetic refusal' });
    });
  }
});
