import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { EmployeeService } from '../employees/services/employee.service';
import { LeaveService } from '../leaves/leave.service';
import { DepartmentService } from '../departments/services/department.service';
import { HrAdminService } from '../hr-admin/hr-admin.service';
import { EmployeeProfileService } from '../profile/employee-profile.service';
import { MyPayrollService } from '../payroll/my-payroll.service';
import { DashboardData, DashboardDataService } from './dashboard-data.service';

describe('DashboardDataService', () => {
  const base = 'http://localhost:8080';
  let role: CrmRole;
  let service: DashboardDataService;
  let http: HttpTestingController;
  let result: DashboardData | undefined;
  beforeEach(() => {
    role = CrmRole.ADMIN; result = undefined;
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(),
      { provide: AuthService, useValue: { hasAnyRole: (roles: CrmRole[]) => roles.includes(role), hasRole: (required: CrmRole) => role === required } },
      { provide: EmployeeService, useValue: { getEmployeesPage: () => of({ totalElements: 25 }) } },
      { provide: LeaveService, useValue: { list: () => of([]) } },
      { provide: DepartmentService, useValue: { getDepartments: () => of([{ id: 1 }]) } },
      { provide: HrAdminService, useValue: { list: () => of({ totalElements: 3 }) } },
      { provide: EmployeeProfileService, useValue: { read: () => of({ employeeId: '7', firstName: 'Ada', lastName: 'Lovelace', jobTitle: 'Analyste', departmentId: null, departmentName: null, hireDate: null }) } },
      { provide: MyPayrollService, useValue: { list: () => of({ totalElements: 2 }) } }
    ] });
    service = TestBed.inject(DashboardDataService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function start() { service.load().subscribe(value => result = value); }
  function flushAdminStats() {
    http.expectOne(base + '/customer/api/customers/statistics').flush({ customers: 4, prospects: 5, suppliers: 2,
      opportunitiesByStage: { NEW: { stage: 'NEW', count: 3, estimatedAmount: 1200 } } });
    http.expectOne(base + '/fees/api/fees/statistics').flush({ count: 6, amount: 900, workedHours: 12,
      invoiceStatuses: {}, paymentStatuses: {}, currencyDefined: false });
    http.expectOne(base + '/invoice/api/invoices/statistics').flush({ invoiceCountsByStatus: { PAID: 2, UNPAID: 1 },
      invoicesByCurrency: { TND: { count: 3, total: 500, paid: 200, outstanding: 300 } },
      quoteCountsByStatus: { ACCEPTED: 1 }, quotesByCurrency: { TND: { count: 1, total: 400 } } });
    http.expectOne(r => r.url === base + '/payroll/api/payrolls').flush({ totalElements: 9 });
  }
  it('loads actual admin aggregates and keeps monetary values grouped by currency', () => {
    start(); flushAdminStats();
    expect(result?.scope).toBe('admin');
    expect(result?.employees.value).toBe(25); expect(result?.departments.value).toBe(1);
    expect(result?.customers.value).toBe(4); expect(result?.contacts.value?.prospects).toBe(5);
    expect(result?.fees.value).toBe(6); expect(result?.feeOverview.value?.currencyDefined).toBeFalse();
    expect(result?.invoices.value?.count).toBe(3);
    expect(result?.invoices.value?.paidAmounts).toEqual([{ currency: 'TND', value: '200,00' }]);
    expect(result?.invoices.value?.outstandingAmounts).toEqual([{ currency: 'TND', value: '300,00' }]);
    expect(result?.invoices.value?.quoteStatuses?.[0].label).toBe('ACCEPTED');
  });
  it('loads only the authenticated employee personal endpoints and finalized evaluation count', () => {
    role = CrmRole.EMPLOYEE; start();
    const evaluation = http.expectOne(r => r.url === base + '/hr/api/evaluations/mine');
    expect(evaluation.request.params.get('size')).toBe('1');
    evaluation.flush({ totalElements: 1, content: [{ id: '9' }] });
    expect(result?.scope).toBe('employee');
    expect(result?.personalProfile.value?.firstName).toBe('Ada');
    expect(result?.personalPayroll.value).toBe(2);
    expect(result?.personalEvaluations.value).toBe(1);
    http.expectNone(r => r.url.includes('/statistics') || r.url.includes('/api/employees'));
  });
  it('loads only client scoped invoice statistics', () => {
    role = CrmRole.CLIENT; start();
    http.expectOne(base + '/invoice/api/invoices/me/statistics').flush({ invoiceCount: 3, paidInvoiceCount: 1,
      unpaidInvoiceCount: 2, byCurrency: [{ currency: 'TND', invoiceCount: 3, total: 300, paid: 100, outstanding: 200 }] });
    expect(result?.scope).toBe('client'); expect(result?.clientInvoices.value?.invoiceCount).toBe(3);
    http.expectNone(r => r.url.includes('/statistics') && !r.url.includes('/me/statistics'));
  });
  it('represents a failed backend aggregate as unavailable instead of zero', () => {
    role = CrmRole.CLIENT; start();
    http.expectOne(base + '/invoice/api/invoices/me/statistics').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(result?.clientInvoices.value).toBeNull();
    expect(result?.clientInvoices.error).toContain('indisponible');
  });
  it('isolates an unavailable admin service without hiding other real aggregates', () => {
    start();
    http.expectOne(base + '/customer/api/customers/statistics').flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne(base + '/fees/api/fees/statistics').flush({ count: 6, amount: 900, workedHours: 12,
      invoiceStatuses: {}, paymentStatuses: {}, currencyDefined: false });
    http.expectOne(base + '/invoice/api/invoices/statistics').flush({ invoiceCountsByStatus: {}, invoicesByCurrency: {},
      quoteCountsByStatus: {}, quotesByCurrency: {} });
    http.expectOne(r => r.url === base + '/payroll/api/payrolls').flush({ totalElements: 0 });
    expect(result?.customers.value).toBeNull(); expect(result?.customers.error).toContain('indisponible');
    expect(result?.fees.value).toBe(6); expect(result?.invoices.value?.count).toBe(0);
  });
  it('keeps employee evaluations unavailable when their personal endpoint fails', () => {
    role = CrmRole.EMPLOYEE; start();
    http.expectOne(r => r.url === base + '/hr/api/evaluations/mine').flush({}, { status: 404, statusText: 'Not Found' });
    expect(result?.personalProfile.value?.employeeId).toBe('7');
    expect(result?.personalPayroll.value).toBe(2);
    expect(result?.personalEvaluations.value).toBeNull();
    expect(result?.personalEvaluations.error).toContain('indisponible');
  });
  it('does not turn an invalid client statistics payload into zero-valued indicators', () => {
    role = CrmRole.CLIENT; start();
    http.expectOne(base + '/invoice/api/invoices/me/statistics').flush({ invoiceCount: 'many', byCurrency: [] });
    expect(result?.clientInvoices.value).toBeNull();
    expect(result?.clientInvoices.error).toContain('indisponible');
  });
});
