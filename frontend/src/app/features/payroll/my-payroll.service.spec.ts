import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PDF_DOWNLOAD_REQUEST } from '../../core/http/pdf-download-context';
import { MyPayrollService } from './my-payroll.service';

describe('MyPayrollService', () => {
  let service: MyPayrollService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/payroll/api/payrolls/me';
  const row = { id: 5, employeeId: '101', baseSalary: 2000, bonuses: 100, deductions: 50,
    taxAmount: 250, netSalary: 1800, payPeriod: '2026-10', paymentDate: '2026-10-31',
    currency: 'TND', status: 'PAID', published: true, createdAt: null, updatedAt: null };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MyPayrollService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('requests only the personal endpoint and applies the period filter on the server', () => {
    service.list(2, '2026-10').subscribe(result => expect(result.content).toEqual([row]));
    const request = http.expectOne(r => r.url === url);
    expect(request.request.method).toBe('GET'); expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('period')).toBe('2026-10'); expect(request.request.params.has('employeeId')).toBeFalse();
    request.flush({ content: [row], number: 2, totalElements: 1, totalPages: 1, size: 10 });
  });

  it('reads a personal detail and rejects identifiers that could alter the path', () => {
    service.read('5').subscribe(result => expect(result).toEqual(row));
    http.expectOne(url + '/5').flush(row);
    expect(() => service.read('5/6')).toThrowError('Invalid payroll id');
  });

  it('downloads PDF only through the personal route', () => {
    const pdf = new Blob(['pdf'], { type: 'application/pdf' });
    service.pdf('5').subscribe(result => expect(result).toBe(pdf));
    const request = http.expectOne(url + '/5/pdf');
    expect(request.request.context.get(PDF_DOWNLOAD_REQUEST)).toBeTrue();
    request.flush(pdf, { headers: { 'Content-Type': 'application/pdf' } });
  });

  it('rejects malformed page contracts returned by API', () => {
    service.list().subscribe({ next: () => fail('Unexpected success'), error: error => expect(error.status).toBe(502) });
    http.expectOne(request => request.url === url).flush({ content: [{ ...row, id: -1 }], number: 0, totalElements: 1, totalPages: 1 });
  });
});
