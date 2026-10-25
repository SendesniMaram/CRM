import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { InvoiceResponse } from '../invoices/invoice.models';
import { DashboardData, DashboardDataService } from './dashboard-data.service';

describe('DashboardDataService with real HTTP services', () => {
  const base = 'http://localhost:8080';
  const urls = { employees: base + '/employee/api/employees', customers: base + '/customer/api/customers',
    fees: base + '/fees/api/fees', payroll: base + '/payroll/api/payrolls',
    leaves: base + '/hr/api/leaves', invoices: base + '/invoice/api/invoices' };
  let role: CrmRole;
  let service: DashboardDataService;
  let http: HttpTestingController;
  let result: DashboardData | undefined;
  beforeEach(() => {
    role = CrmRole.ADMIN; result = undefined;
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(),
      { provide: AuthService, useValue: { hasAnyRole: (roles: CrmRole[]) => roles.includes(role), hasRole: (required: CrmRole) => role === required } }
    ] });
    service = TestBed.inject(DashboardDataService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  function counts(failEmployees = false) {
    for (const key of ['employees', 'customers', 'fees', 'payroll'] as const) {
      const request = http.expectOne(r => r.url === urls[key]);
      expect(request.request.method).toBe('GET'); expect(request.request.params.get('size')).toBe('1');
      expect(request.request.params.get('sortBy')).toBe('id');
      if (key === 'employees' && failEmployees) request.flush({}, { status: 503, statusText: 'Unavailable' });
      else request.flush({ totalElements: key === 'employees' ? 125 : 42, content: [] });
    }
    http.expectOne(urls.leaves).flush([]);
    http.expectOne(base + '/department/api/departments').flush([{id:1}]);
    for(const module of ['trainings','attendances','hr-records']) {
      const request=http.expectOne(r=>r.url===base+'/hr/api/'+module);
      expect(request.request.params.get('sortBy')).toBe('id');
      request.flush(JSON.stringify({content:[],totalElements:42,totalPages:5,number:0}));
    }
  }
  function invoice(id: number, totalAmount = .1, currency: string | null = 'TND'): InvoiceResponse {
    return { id, invoiceNumber: 'TEST-' + id, customerId: '1', subtotal: 1, tax: null, totalAmount,
      currency, status: id % 2 ? 'PAYÉE' : 'BROUILLON', issueDate: null, dueDate: null, notes: null, createdAt: null, updatedAt: null };
  }
  function page(number: number, total: number, rows: InvoiceResponse[]) {
    const request = http.expectOne(r => r.url === urls.invoices && r.params.get('page') === String(number));
    expect(request.request.params.get('size')).toBe('100');
    expect(request.request.params.get('sortBy')).toBe('id');
    request.flush({ content: rows, number, size: 100, totalElements: total, totalPages: Math.ceil(total / 100) });
  }
  function start() { service.load().subscribe(value => result = value); }

  for (const admin of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it(`loads administrative counts from totalElements for ${admin} and handles an empty invoice set`, () => {
      role = admin; start(); counts(); page(0, 0, []);
      expect(result?.employees.value).toBe(125); expect(result?.customers.value).toBe(42);
      expect(result?.fees.value).toBe(42); expect(result?.payroll.value).toBe(42);
      expect(result?.departments.value).toBe(1); expect(result?.trainings.value).toBe(42);
      expect(result?.attendances.value).toBe(42); expect(result?.hrRecords.value).toBe(42);
      expect(result?.invoices.value).toEqual({ count: 0, statuses: [], amounts: [], reason: '' });
    });
  }
  it('reads every invoice page before calculating totals or status distributions', () => {
    start(); counts(); page(0, 101, Array.from({ length: 100 }, (_, i) => invoice(i + 1)));
    expect(result).toBeUndefined();
    page(1, 101, [invoice(101, .2)]);
    expect(result?.invoices.value?.count).toBe(101);
    expect(result?.invoices.value?.amounts).toEqual([{ currency: 'TND', value: '10,20' }]);
    expect(result?.invoices.value?.statuses?.reduce((sum, item) => sum + item.count, 0)).toBe(101);
  });
  it('keeps currencies separate and adds monetary cents without binary rounding artifacts', () => {
    start(); counts(); page(0, 3, [invoice(1, .1), invoice(2, .2), invoice(3, 5, 'EUR')]);
    expect(result?.invoices.value?.amounts).toEqual([{ currency: 'EUR', value: '5,00' }, { currency: 'TND', value: '0,30' }]);
  });
  it('does not assume a currency when any invoice lacks one', () => {
    start(); counts(); page(0, 1, [invoice(1, 5, null)]);
    expect(result?.invoices.value?.amounts).toBeNull(); expect(result?.invoices.value?.statuses?.length).toBe(1);
  });
  it('retains the count but refuses aggregates above the bounded 1000-row scan', () => {
    start(); counts(); page(0, 1001, Array.from({ length: 100 }, (_, i) => invoice(i + 1)));
    expect(result?.invoices.value?.count).toBe(1001); expect(result?.invoices.value?.amounts).toBeNull();
    expect(result?.invoices.value?.statuses).toBeNull(); http.expectNone(r => r.url === urls.invoices);
  });
  it('never presents a partial total if another invoice page fails', () => {
    start(); counts(); page(0, 101, Array.from({ length: 100 }, (_, i) => invoice(i + 1)));
    http.expectOne(r => r.url === urls.invoices).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(result?.invoices.value?.count).toBe(101); expect(result?.invoices.value?.amounts).toBeNull();
  });
  it('rejects totals that change during pagination', () => {
    start(); counts(); page(0, 101, Array.from({ length: 100 }, (_, i) => invoice(i + 1)));
    page(1, 102, [invoice(101), invoice(102)]);
    expect(result?.invoices.value?.statuses).toBeNull(); expect(result?.invoices.value?.amounts).toBeNull();
  });
  it('rejects duplicate invoice IDs and truncated pages', () => {
    start(); counts(); page(0, 2, [invoice(1), invoice(1)]);
    expect(result?.invoices.value?.amounts).toBeNull();
    start(); counts(); page(0, 2, [invoice(1)]);
    expect(result?.invoices.value?.amounts).toBeNull();
  });
  it('isolates a failed microservice and never turns failure into a zero', () => {
    start(); counts(true); page(0, 0, []);
    expect(result?.employees.value).toBeNull(); expect(result?.employees.error).toContain('indisponible');
    expect(result?.customers.value).toBe(42);
  });
  it('reports a missing employee association as unavailable without administrative fallback', () => {
    role = CrmRole.EMPLOYEE; start();
    http.expectOne(urls.leaves).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(result?.leaves.value).toBeNull(); expect(result?.employees.value).toBeNull();
    http.expectNone(r => r.url !== urls.leaves);
  });
  it('calls only the backend-scoped leave list for EMPLOYEE', () => {
    role = CrmRole.EMPLOYEE; start();
    http.expectOne(urls.leaves).flush([{ id: 1, employeeId: '7', status: 'PENDING', createdAt: '2026-10-16T10:00:00' }]);
    expect(result?.scope).toBe('employee'); expect(result?.leaves.value?.length).toBe(1);
    http.expectNone(r => r.url !== urls.leaves);
  });
  it('makes no administrative or personal API request for CLIENT', () => {
    role = CrmRole.CLIENT; start(); expect(result?.scope).toBe('client'); http.expectNone(() => true);
  });
  it('times out unavailable services and completes instead of leaving loading forever', () => {
    jasmine.clock().install();
    try {
      role = CrmRole.EMPLOYEE; start(); jasmine.clock().tick(10001);
      expect(result?.leaves.value).toBeNull(); expect(result?.leaves.error).toContain('indisponible');
    } finally { jasmine.clock().uninstall(); }
  });
});
