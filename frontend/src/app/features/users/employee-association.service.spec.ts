import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeAssociationService } from './employee-association.service';
import { canonicalId, parseDirectory } from './association-identifiers';

describe('EmployeeAssociationService canonical identifiers and contracts', () => {
  let service: EmployeeAssociationService; let http: HttpTestingController;
  const base = 'http://localhost:8080/hr/api/employee-associations';
  const options = 'http://localhost:8080/hr/api/leaves/associations/options';
  const link = { userId: '9007199254740993', employeeId: '9223372036854775807' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EmployeeAssociationService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('reads the selected uid without any client identity header', () => {
    service.read(link.userId).subscribe(value => expect(value).toEqual(link));
    const req = http.expectOne(`${base}/users/${link.userId}`);
    expect(req.request.method).toBe('GET'); expect(req.request.headers.keys()).toEqual([]); req.flush(link);
  });
  for (const status of [200, 201]) {
    it('accepts idempotent/new creation ' + status + ' with string IDs', () => {
      service.create(link.userId, link.employeeId).subscribe(value => {
        expect(value.association).toEqual(link); expect(value.created).toBe(status === 201);
      });
      const req = http.expectOne(base); expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual(link);
      req.flush(link, { status, statusText: 'OK' });
    });
  }
  for (const status of [400, 401, 403, 404, 409, 502, 503]) {
    it('preserves HTTP error ' + status, () => {
      service.create('42', '103').subscribe({ next: () => fail('Unexpected success'), error: error => expect(error.status).toBe(status) });
      http.expectOne(base).flush({}, { status, statusText: 'Synthetic refusal' });
    });
  }
  for (const invalid of ['', '01', '0', '-1', '1.0', '1e2', ' 42', '9223372036854775808', 42]) {
    it('rejects a noncanonical/out-of-range input ' + invalid + ' before HTTP', () => {
      expect(canonicalId(invalid)).toBeFalse();
      service.create(invalid as string, '103').subscribe({ error: error => expect(error.status).toBe(400), next: () => fail('Unexpected success') });
      http.expectNone(base);
    });
  }
  for (const body of [null, { userId: 42, employeeId: '103' }, { userId: '43', employeeId: '103' },
    { userId: '42', employeeId: '01' }, { userId: '42', employeeId: '104' }]) {
    it('does not confirm a mismatched or malformed mutation ' + JSON.stringify(body), () => {
      service.create('42', '103').subscribe({ error: error => expect(error.status).toBe(502), next: () => fail('Unexpected success') });
      http.expectOne(base).flush(body, { status: 201, statusText: 'Created' });
    });
  }
  it('rejects an unexpected success code', () => {
    service.create('42', '103').subscribe({ error: error => expect(error.status).toBe(502), next: () => fail('Unexpected success') });
    http.expectOne(base).flush(null, { status: 204, statusText: 'No Content' });
  });
  it('preserves long IDs from legacy numeric directory JSON without rounding', () => {
    service.options().subscribe(value => {
      expect(value.users[0].id).toBe(link.userId); expect(value.employees[0].id).toBe(link.employeeId);
    });
    http.expectOne(options).flush('{"users":[{"id":9007199254740993,"label":"test-user"}],"employees":[{"id":9223372036854775807,"label":"EMP-103"}]}');
  });
  it('does not interpret id-looking text inside labels as a JSON field', () => {
    const label = 'id: 9007199254740993; "id":42';
    expect(parseDirectory(JSON.stringify({ id: 42, label }))).toEqual({ id: '42', label });
  });
  for (const body of ['{', '{"users":[],"employees":[{"id":1.2,"label":"invalid"}]}',
    '{"users":[{"id":1,"label":"x"},{"id":1,"label":"y"}],"employees":[]}', '{"users":[],"employees":null}']) {
    it('rejects invalid directory data ' + body, () => {
      service.options().subscribe({ error: error => expect(error.status).toBe(502), next: () => fail('Unexpected success') });
      http.expectOne(options).flush(body);
    });
  }
});
