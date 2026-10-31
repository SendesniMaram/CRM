import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, shareReplay, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { EmployeeService } from '../employees/services/employee.service';
import { LeaveResponse } from '../leaves/leave.models';
import { LeaveService } from '../leaves/leave.service';
import { DepartmentService } from '../departments/services/department.service';
import { HrAdminService } from '../hr-admin/hr-admin.service';
import { EmployeeProfileService, EmployeeProfile } from '../profile/employee-profile.service';
import { MyPayrollService } from '../payroll/my-payroll.service';

export type DashboardScope = 'admin' | 'employee' | 'client';
export interface Reading<T> { value: T | null; error: string }
export interface Distribution { label: string; count: number; percent: number }
export interface MoneyMetric { currency: string; invoiceCount: number; total: number; paid: number; outstanding: number }
export interface ContactOverview {
  customers: number; prospects: number; suppliers: number;
  opportunitiesByStage: Record<string, { stage: string; count: number; estimatedAmount: number }>;
}
export interface InvoiceOverview {
  count: number; statuses: Distribution[] | null; amounts: { currency: string; value: string }[] | null;
  paidAmounts?: { currency: string; value: string }[] | null;
  outstandingAmounts?: { currency: string; value: string }[] | null;
  quoteStatuses?: Distribution[] | null; quoteAmounts?: { currency: string; value: string }[] | null;
  reason: string;
}
export interface FeeOverview { count: number; amount: number; workedHours: number; invoiceStatuses: Distribution[]; paymentStatuses: Distribution[]; currencyDefined: false }
export interface ClientInvoiceOverview { invoiceCount: number; paidInvoiceCount: number; unpaidInvoiceCount: number; byCurrency: MoneyMetric[] }
export interface DashboardData {
  scope: DashboardScope; employees: Reading<number>; departments: Reading<number>; trainings: Reading<number>;
  attendances: Reading<number>; hrRecords: Reading<number>; contracts: Reading<number>; evaluations: Reading<number>;
  jobOffers: Reading<number>; applications: Reading<number>; customers: Reading<number>; fees: Reading<number>;
  payroll: Reading<number>; leaves: Reading<LeaveResponse[]>; invoices: Reading<InvoiceOverview>;
  contacts: Reading<ContactOverview>; feeOverview: Reading<FeeOverview>;
  personalProfile: Reading<EmployeeProfile>; personalPayroll: Reading<number>; personalEvaluations: Reading<number>;
  clientInvoices: Reading<ClientInvoiceOverview>;
}
const unavailable = <T>(): Reading<T> => ({ value: null, error: '' });
export function emptyDashboard(scope: DashboardScope): DashboardData {
  return { scope, employees: unavailable(), departments: unavailable(), trainings: unavailable(), attendances: unavailable(),
    hrRecords: unavailable(), contracts: unavailable(), evaluations: unavailable(), jobOffers: unavailable(), applications: unavailable(),
    customers: unavailable(), fees: unavailable(), payroll: unavailable(), leaves: unavailable(),
    invoices: unavailable(), contacts: unavailable(), feeOverview: unavailable(), personalProfile: unavailable(),
    personalPayroll: unavailable(), personalEvaluations: unavailable(), clientInvoices: unavailable() };
}
export function distribution(values: string[]): Distribution[] {
  const counts = new Map<string, number>();
  values.forEach(value => counts.set(value, (counts.get(value) ?? 0) + 1));
  return [...counts].sort(([a], [b]) => a.localeCompare(b)).map(([label, count]) => ({ label, count, percent: values.length ? count / values.length * 100 : 0 }));
}

@Injectable({ providedIn: 'root' })
export class DashboardDataService {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly employees = inject(EmployeeService);
  private readonly leaves = inject(LeaveService);
  private readonly departments = inject(DepartmentService);
  private readonly hr = inject(HrAdminService);
  private readonly profile = inject(EmployeeProfileService);
  private readonly myPayroll = inject(MyPayrollService);
  scope(): DashboardScope {
    return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) ? 'admin'
      : this.auth.hasRole(CrmRole.EMPLOYEE) ? 'employee' : 'client';
  }
  private read<T>(request: Observable<T>): Observable<Reading<T>> {
    return request.pipe(timeout(10000), map(value => ({ value, error: '' })),
      catchError(() => of({ value: null, error: 'Service indisponible. Réessayez.' })));
  }
  private count(page: { totalElements: number }): number {
    if (!Number.isSafeInteger(page.totalElements) || page.totalElements < 0) throw new Error('Invalid count');
    return page.totalElements;
  }
  load(): Observable<DashboardData> {
    const scope = this.scope();
    const empty = emptyDashboard(scope);
    if (scope === 'client') return this.read(this.http.get<unknown>('http://localhost:8080/invoice/api/invoices/me/statistics')
      .pipe(map(value => validateClientInvoices(value)))).pipe(map(clientInvoices => ({ ...empty, clientInvoices })));
    const leaveRows = this.read(this.leaves.list().pipe(map(rows => {
      if (!Array.isArray(rows) || new Set(rows.map(row => row.id)).size !== rows.length
        || rows.some(row => !Number.isSafeInteger(row.id) || row.id <= 0 || !['PENDING', 'APPROVED', 'REJECTED'].includes(row.status))) throw new Error('Invalid leave list');
      return rows;
    })));
    if (scope === 'employee') return forkJoin({
      leaves: leaveRows,
      personalProfile: this.read(this.profile.read()),
      personalPayroll: this.read(this.myPayroll.list(0).pipe(map(page => this.count(page)))),
      personalEvaluations: this.read(this.http.get<unknown>('http://localhost:8080/hr/api/evaluations/mine', { params: { page: 0, size: 1 } })
        .pipe(map(value => validatePageCount(value))))
    }).pipe(map(result => ({ ...empty, ...result })));
    const contacts = this.read(this.http.get<ContactOverview>('http://localhost:8080/customer/api/customers/statistics').pipe(map(value => {
      if (!value || !Number.isSafeInteger(value.customers) || !Number.isSafeInteger(value.prospects) || !Number.isSafeInteger(value.suppliers)
          || !value.opportunitiesByStage || typeof value.opportunitiesByStage !== 'object') throw new Error('Invalid contacts statistics');
      return value;
    }))).pipe(shareReplay(1));
    const fees = this.read(this.http.get<Omit<FeeOverview, 'invoiceStatuses' | 'paymentStatuses'> & {
      invoiceStatuses: Record<string, number>; paymentStatuses: Record<string, number>
    }>('http://localhost:8080/fees/api/fees/statistics').pipe(map(value => {
      if (!value || !Number.isSafeInteger(value.count) || value.count < 0 || !Number.isFinite(value.amount)
        || !Number.isFinite(value.workedHours) || value.workedHours < 0
        || !value.invoiceStatuses || typeof value.invoiceStatuses !== 'object'
        || !value.paymentStatuses || typeof value.paymentStatuses !== 'object') throw new Error('Invalid fee statistics');
      return { ...value, invoiceStatuses: countDistribution(value.invoiceStatuses), paymentStatuses: countDistribution(value.paymentStatuses) };
    }))).pipe(shareReplay(1));
    return forkJoin({
      departments: this.read(this.departments.getDepartments().pipe(map(rows => {
        if (!Array.isArray(rows) || rows.some(row => !Number.isSafeInteger(row.id) || row.id <= 0)
          || new Set(rows.map(row => row.id)).size !== rows.length) throw new Error('Invalid departments');
        return rows.length;
      }))),
      trainings: this.read(this.hr.list('trainings').pipe(map(page => this.count(page)))),
      attendances: this.read(this.hr.list('attendances').pipe(map(page => this.count(page)))),
      hrRecords: this.read(this.hr.list('hr-records').pipe(map(page => this.count(page)))),
      contracts: this.read(this.hr.list('contracts').pipe(map(page => this.count(page)))),
      evaluations: this.read(this.hr.list('evaluations').pipe(map(page => this.count(page)))),
      jobOffers: this.read(this.hr.list('job-offers').pipe(map(page => this.count(page)))),
      applications: this.read(this.hr.list('applications').pipe(map(page => this.count(page)))),
      employees: this.read(this.employees.getEmployeesPage({ page: 0, size: 1, sortBy: 'id', direction: 'asc' }).pipe(map(page => this.count(page)))),
      customers: contacts.pipe(map(result => ({ value: result.value?.customers ?? null, error: result.error }))),
      fees: fees.pipe(map(result => ({ value: result.value?.count ?? null, error: result.error }))),
      payroll: this.read(this.http.get<{ totalElements: number }>('http://localhost:8080/payroll/api/payrolls', { params: { page: 0, size: 1, sortBy: 'id', direction: 'asc' } }).pipe(map(page => this.count(page)))),
      leaves: leaveRows,
      invoices: this.read(this.http.get<unknown>('http://localhost:8080/invoice/api/invoices/statistics').pipe(map(value => validateInvoiceOverview(value)))),
      feeOverview: fees,
      contacts
    }).pipe(map(result => ({ ...empty, ...result })));
  }
}

function validatePageCount(value: unknown): number {
  const page = value as { totalElements?: unknown } | null;
  if (!page || !Number.isSafeInteger(page.totalElements) || Number(page.totalElements) < 0) throw new Error('Invalid page');
  return Number(page.totalElements);
}
function validateClientInvoices(value: unknown): ClientInvoiceOverview {
  const stats = value as ClientInvoiceOverview | null;
  if (!stats || !Number.isSafeInteger(stats.invoiceCount) || !Number.isSafeInteger(stats.paidInvoiceCount)
      || !Number.isSafeInteger(stats.unpaidInvoiceCount) || !Array.isArray(stats.byCurrency)) throw new Error('Invalid personal statistics');
  return stats;
}
function formatMoney(value: number): string { return (value / 100).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function countDistribution(values: Record<string, number>): Distribution[] {
  const rows = Object.entries(values).map(([label, count]) => ({ label, count, percent: 0 }));
  const total = rows.reduce((sum, row) => sum + row.count, 0);
  rows.forEach(row => row.percent = total ? row.count / total * 100 : 0);
  return rows;
}
function validateInvoiceOverview(value: unknown): InvoiceOverview {
  const raw = value as { invoiceCountsByStatus?: Record<string, number>; invoicesByCurrency?: Record<string, { count: number; total: number; paid: number; outstanding: number }>;
    quoteCountsByStatus?: Record<string, number>; quotesByCurrency?: Record<string, { count: number; total: number }> } | null;
  if (!raw || !raw.invoiceCountsByStatus || !raw.invoicesByCurrency || !raw.quoteCountsByStatus || !raw.quotesByCurrency) throw new Error('Invalid invoice statistics');
  const statuses = Object.entries(raw.invoiceCountsByStatus).map(([label, count]) => ({ label, count, percent: 0 }));
  const totalCount = statuses.reduce((sum, row) => sum + row.count, 0);
  statuses.forEach(row => row.percent = totalCount ? row.count / totalCount * 100 : 0);
  const amounts = Object.entries(raw.invoicesByCurrency).map(([currency, row]) => ({ currency, value: formatMoney(Math.round(row.total * 100)) }));
  const paidAmounts = Object.entries(raw.invoicesByCurrency).map(([currency, row]) => ({ currency, value: formatMoney(Math.round(row.paid * 100)) }));
  const outstandingAmounts = Object.entries(raw.invoicesByCurrency).map(([currency, row]) => ({ currency, value: formatMoney(Math.round(row.outstanding * 100)) }));
  const quoteStatuses = Object.entries(raw.quoteCountsByStatus).map(([label, count]) => ({ label, count, percent: 0 }));
  const quoteCount = quoteStatuses.reduce((sum, row) => sum + row.count, 0); quoteStatuses.forEach(row => row.percent = quoteCount ? row.count / quoteCount * 100 : 0);
  return { count: totalCount, statuses, amounts, paidAmounts, outstandingAmounts, quoteStatuses,
    quoteAmounts: Object.entries(raw.quotesByCurrency).map(([currency, row]) => ({ currency, value: formatMoney(Math.round(row.total * 100)) })), reason: '' };
}
