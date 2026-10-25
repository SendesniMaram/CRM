import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { LeaveService } from './leave.service';
describe('LeaveService', () => {
  let service: LeaveService; let http: HttpTestingController;
  const base = 'http://localhost:8080/hr/api/leaves';
  const body = { startDate: '2099-01-01', endDate: '2099-01-02', reason: null };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(LeaveService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('lists without caller-supplied ownership filters', () => {
    service.list().subscribe(rows => expect(rows).toEqual([]));
    const req = http.expectOne(base); expect(req.request.method).toBe('GET'); req.flush([]);
  });
  it('gets a detail', () => { service.get(7).subscribe(); const req = http.expectOne(`${base}/7`); expect(req.request.method).toBe('GET'); req.flush({ id: 7 }); });
  it('creates only the personal dates/reason DTO', () => {
    service.create(body).subscribe(); const req = http.expectOne(`${base}/mine`);
    expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual(body); expect(req.request.body.employeeId).toBeUndefined(); req.flush({});
  });
  it('updates a personal pending request', () => { service.update(7, body).subscribe(); const req = http.expectOne(`${base}/mine/7`); expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual(body); req.flush({}); });
  it('deletes by ID', () => { service.delete(7).subscribe(); const req = http.expectOne(`${base}/7`); expect(req.request.method).toBe('DELETE'); req.flush(null, { status: 204, statusText: 'No Content' }); });
  for (const action of ['approve', 'reject'] as const) {
    it(`${action} does not send a validator`, () => { service[action](7).subscribe(); const req = http.expectOne(`${base}/7/${action}`); expect(req.request.method).toBe('PATCH'); expect(req.request.body).toEqual({}); req.flush({}); });
  }
  it('reads association options', () => { service.options().subscribe(value => expect(value.users).toEqual([])); const req = http.expectOne(`${base}/associations/options`); expect(req.request.method).toBe('GET'); req.flush({ users: [], employees: [] }); });
  it('lists associations', () => { service.associations().subscribe(value => expect(value).toEqual([])); const req = http.expectOne(`${base}/associations`); expect(req.request.method).toBe('GET'); req.flush([]); });
  it('sends administrator-selected existing IDs for association', () => { service.associate(3, 8).subscribe(); const req = http.expectOne(`${base}/associations`); expect(req.request.body).toEqual({ userId: 3, employeeId: 8 }); req.flush({}); });
  it('propagates access errors', () => { service.list().subscribe({ error: e => expect(e.status).toBe(403) }); http.expectOne(base).flush({}, { status: 403, statusText: 'Forbidden' }); });
  it('reassigns only explicitly selected IDs without moving leave records', () => {
    service.reassign(42, 103).subscribe(); const req = http.expectOne(`${base}/associations/42`);
    expect(req.request.method).toBe('PUT'); expect(req.request.body).toEqual({ employeeId: 103 });
    req.flush({ userId: 42, employeeId: '103', username: 'test' });
  });
  for (const status of [400, 401, 403, 409, 502, 503]) {
    it('preserves reassignment failure status ' + status, () => {
      service.reassign(42, 103).subscribe({ next: () => fail('Unexpected success'), error: e => expect(e.status).toBe(status) });
      http.expectOne(`${base}/associations/42`).flush({}, { status, statusText: 'Synthetic error' });
    });
  }
});
