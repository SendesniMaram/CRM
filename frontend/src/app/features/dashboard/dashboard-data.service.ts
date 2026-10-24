import { Injectable, inject } from '@angular/core';
import { Observable, catchError, defer, forkJoin, map, of, switchMap, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { CustomerService } from '../customers/customer.service';
import { EmployeeService } from '../employees/services/employee.service';
import { FeeService } from '../fees/fee.service';
import { InvoicePage, InvoiceResponse } from '../invoices/invoice.models';
import { InvoiceService } from '../invoices/invoice.service';
import { LeaveResponse } from '../leaves/leave.models';
import { LeaveService } from '../leaves/leave.service';
import { PayrollService } from '../payroll/payroll.service';
import { DepartmentService } from '../departments/services/department.service';
import { HrAdminService } from '../hr-admin/hr-admin.service';

export type DashboardScope = 'admin' | 'employee' | 'client';
export interface Reading<T> { value: T | null; error: string }
export interface Distribution { label: string; count: number; percent: number }
export interface InvoiceOverview {
  count: number;
  statuses: Distribution[] | null;
  amounts: { currency: string; value: string }[] | null;
  reason: string;
}
export interface DashboardData {
  scope: DashboardScope;
  employees: Reading<number>;
  departments: Reading<number>;
  trainings: Reading<number>;
  attendances: Reading<number>;
  hrRecords: Reading<number>;
  customers: Reading<number>;
  fees: Reading<number>;
  payroll: Reading<number>;
  leaves: Reading<LeaveResponse[]>;
  invoices: Reading<InvoiceOverview>;
}
const unavailable = <T>(): Reading<T> => ({ value: null, error: '' });
export function emptyDashboard(scope: DashboardScope): DashboardData {
  return { scope, employees: unavailable<number>(), departments: unavailable<number>(), trainings: unavailable<number>(),
    attendances: unavailable<number>(), hrRecords: unavailable<number>(), customers: unavailable<number>(), fees: unavailable<number>(),
    payroll: unavailable<number>(), leaves: unavailable<LeaveResponse[]>(), invoices: unavailable<InvoiceOverview>() };
}
export function distribution(values: string[]): Distribution[] {
  const counts = new Map<string, number>();
  values.forEach(value => counts.set(value, (counts.get(value) ?? 0) + 1));
  return [...counts].sort(([a], [b]) => a.localeCompare(b)).map(([label, count]) => ({
    label, count, percent: values.length ? count / values.length * 100 : 0
  }));
}

@Injectable({ providedIn: 'root' })
export class DashboardDataService {
  private readonly auth = inject(AuthService);
  private readonly employees = inject(EmployeeService);
  private readonly customers = inject(CustomerService);
  private readonly fees = inject(FeeService);
  private readonly payroll = inject(PayrollService);
  private readonly leaves = inject(LeaveService);
  private readonly invoices = inject(InvoiceService);
  private readonly departments = inject(DepartmentService);
  private readonly hr = inject(HrAdminService);
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
    return defer(() => {
      const scope = this.scope();
      const empty = emptyDashboard(scope);
      if (scope === 'client') return of(empty);
      const leaves = this.read(this.leaves.list().pipe(map(rows => {
        if (!Array.isArray(rows) || new Set(rows.map(row => row.id)).size !== rows.length
          || rows.some(row => !Number.isSafeInteger(row.id) || row.id <= 0
            || !['PENDING', 'APPROVED', 'REJECTED'].includes(row.status))) throw new Error('Invalid leave list');
        return rows;
      })));
      if (scope === 'employee') return leaves.pipe(map(result => ({ ...empty, leaves: result })));
      return forkJoin({
        departments: this.read(this.departments.getDepartments().pipe(map(rows => {
          if (!Array.isArray(rows) || rows.some(row => !Number.isSafeInteger(row.id) || row.id <= 0)
            || new Set(rows.map(row => row.id)).size !== rows.length) throw new Error('Invalid departments');
          return rows.length;
        }))),
        trainings: this.read(this.hr.list('trainings').pipe(map(page => this.count(page)))),
        attendances: this.read(this.hr.list('attendances').pipe(map(page => this.count(page)))),
        hrRecords: this.read(this.hr.list('hr-records').pipe(map(page => this.count(page)))),
        employees: this.read(this.employees.getEmployeesPage({ page: 0, size: 1, sortBy: 'id', direction: 'asc' }).pipe(map(page => this.count(page)))),
        customers: this.read(this.customers.list({ page: 0, size: 1, sortBy: 'id', direction: 'asc' }).pipe(map(page => this.count(page)))),
        fees: this.read(this.fees.list({ page: 0, size: 1, sortBy: 'id', direction: 'asc' }).pipe(map(page => this.count(page)))),
        payroll: this.read(this.payroll.list({ page: 0, size: 1, sortBy: 'id', direction: 'asc' }).pipe(map(page => this.count(page)))),
        leaves, invoices: this.read(this.invoiceOverview())
      }).pipe(map(result => ({ scope, ...result })));
    });
  }
  private invoiceOverview(): Observable<InvoiceOverview> {
    const page = (number: number) => this.invoices.list({ page: number, size: 100, sortBy: 'id', direction: 'asc' });
    return page(0).pipe(switchMap(first => {
      const count = this.count(first);
      const partial = (reason: string): InvoiceOverview => ({ count, statuses: null, amounts: null, reason });
      if (count > 1000) return of(partial('Au-delà de 1 000 factures, une agrégation serveur est nécessaire.'));
      const pages = Math.max(1, Math.ceil(count / 100));
      const validate = (result: InvoicePage, index: number) => {
        if (result.totalElements !== count || result.number !== index || result.size !== 100
          || result.totalPages !== Math.ceil(count / 100) || !Array.isArray(result.content)
          || result.content.length !== Math.min(100, Math.max(0, count - index * 100))) throw new Error('Inconsistent pages');
        return result.content;
      };
      let request: Observable<InvoiceResponse[]> = defer(() => of(validate(first, 0)));
      for (let index = 1; index < pages; index++) {
        const current = index;
        request = request.pipe(switchMap(rows => page(current).pipe(timeout(10000), map(next => [...rows, ...validate(next, current)]))));
      }
      return request.pipe(map(rows => {
        if (rows.some(row => !Number.isSafeInteger(row.id) || row.id <= 0)
          || new Set(rows.map(row => row.id)).size !== count) throw new Error('Duplicate or invalid IDs');
        const statuses = distribution(rows.map(row => row.status?.trim() || 'Non renseigné'));
        const totals = new Map<string, number>();
        let validAmounts = true;
        for (const row of rows) {
          const currency = row.currency?.trim();
          const cents = Math.round(row.totalAmount * 100);
          const total = (totals.get(currency ?? '') ?? 0) + cents;
          if (!currency || !Number.isFinite(row.totalAmount) || !Number.isSafeInteger(cents)
            || !Number.isSafeInteger(total)) validAmounts = false;
          totals.set(currency ?? '', total);
        }
        return { count, statuses, amounts: validAmounts ? [...totals].sort(([a], [b]) => a.localeCompare(b))
          .map(([currency, cents]) => ({ currency, value: (cents / 100).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) })) : null,
          reason: validAmounts ? '' : 'Devise absente ou montant invalide : total monétaire indisponible.' };
      }), catchError(() => of(partial('Lecture incomplète ou données modifiées pendant le chargement. Réessayez.'))));
    }));
  }
}
