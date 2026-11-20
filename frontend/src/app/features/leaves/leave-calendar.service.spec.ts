import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LeaveCalendarService } from './leave-calendar.service';

describe('LeaveCalendarService', () => {
  let service: LeaveCalendarService; let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(LeaveCalendarService);http=TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('loads a month and optional employee filter through Gateway', () => {
    service.list('2026-10-01','2026-10-31','  101 ').subscribe(rows=>expect(rows.length).toBe(0));
    const req=http.expectOne(r=>r.url==='http://localhost:8080/hr/api/leaves/calendar');
    expect(req.request.params.get('from')).toBe('2026-10-01');expect(req.request.params.get('to')).toBe('2026-10-31');expect(req.request.params.get('employeeId')).toBe('101');req.flush([]);
  });
  it('rejects an invalid date range before sending a request', () => { expect(()=>service.list('2026-11-01','2026-10-01')).toThrowError('Invalid calendar range'); });
});
