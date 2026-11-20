import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserDirectoryService } from './user-directory.service';

describe('UserDirectoryService', () => {
  let service: UserDirectoryService; let http: HttpTestingController;
  const url = 'http://localhost:8080/identity/api/users';
  const request = { firstName: 'Test', lastName: 'Employee', username: 'test-user', email: 'test@example.test', password: 'test-only-password', phone: '' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserDirectoryService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('preserves a long numeric account ID without rounding', () => {
    service.list().subscribe(rows => expect(rows[0].id).toBe('9223372036854775807'));
    http.expectOne(url).flush('[{"id":9223372036854775807,"username":"long-id","email":"test@example.test","enabled":true,"accountNonLocked":true,"roles":["EMPLOYEE"]}]');
  });
  for (const invalid of ['0', '01', '9223372036854775808']) {
    it('rejects a noncanonical or unsupported directory ID ' + invalid, () => {
      service.list().subscribe({ next: () => fail('Unexpected success'), error: error => expect(error.status).toBe(502) });
      http.expectOne(url).flush(JSON.stringify([{ id: invalid, username: 'test', email: 'test@example.test', enabled: true, accountNonLocked: true, roles: ['EMPLOYEE'] }]));
    });
  }
  it('uses the full directory roles rather than roleType', () => {
    const rows = [{ id: 42, username: 'test', email: 'test@example.test', enabled: true, accountNonLocked: false,
      roles: ['ADMIN', 'EMPLOYEE'], roleType: 'ADMIN' }];
    service.list().subscribe(value => expect(value as unknown).toEqual(rows.map(row => ({ ...row, id: String(row.id) }))));
    const req = http.expectOne(url); expect(req.request.method).toBe('GET'); req.flush(JSON.stringify(rows));
  });
  it('sends only allowed creation fields and accepts exactly 201', () => {
    let complete = false;
    service.createEmployee({ ...request, roles: ['SUPER_ADMIN'], enabled: false } as typeof request).subscribe(() => complete = true);
    const req = http.expectOne(url); expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual(request);
    req.flush(null, { status: 201, statusText: 'Created' }); expect(complete).toBeTrue();
  });
  it('rejects an unexpected success response rather than confirming creation', () => {
    service.createEmployee(request).subscribe({ next: () => fail('Unexpected success'), error: e => expect(e.status).toBe(502) });
    http.expectOne(url).flush(null, { status: 200, statusText: 'OK' });
  });
  it('creates a requested non-employee role through the admin endpoint', () => {
    service.create({ ...request, roleType: 'CLIENT' as any }).subscribe();
    const req = http.expectOne(`${url}/admin`); expect(req.request.method).toBe('POST');
    expect(req.request.body.roleType).toBe('CLIENT'); req.flush({ id: '9', username: request.username });
  });
  it('updates profile, roles and enabled state through dedicated methods', () => {
    service.update('9', { firstName: 'A', lastName: 'B', email: 'a@example.test', phone: null }).subscribe();
    let req = http.expectOne(`${url}/9`); expect(req.request.method).toBe('PUT'); req.flush({});
    service.setRoles('9', ['CLIENT' as any]).subscribe();
    req = http.expectOne(`${url}/9/roles`); expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual({ roles: ['CLIENT'] }); req.flush({});
    service.setEnabled('9', false).subscribe();
    req = http.expectOne(`${url}/9/status`); expect(req.request.method).toBe('PATCH'); expect(req.request.body).toEqual({ enabled: false }); req.flush({});
  });
  for (const status of [400, 401, 403, 409, 502, 503]) {
    it('propagates creation error ' + status, () => {
      service.createEmployee(request).subscribe({ next: () => fail('Unexpected success'), error: e => expect(e.status).toBe(status) });
      http.expectOne(url).flush({}, { status, statusText: 'Synthetic failure' });
    });
  }
  for (const body of [{}, null, [{ id: 1, roles: 'EMPLOYEE' }], [{ id: 1, username: 'test', email: 'x', enabled: true, roles: [] }]]) {
    it('rejects a malformed directory response ' + JSON.stringify(body), () => {
      service.list().subscribe({ next: () => fail('Unexpected success'), error: e => expect(e.status).toBe(502) });
      http.expectOne(url).flush(JSON.stringify(body));
    });
  }
});
