import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, map, of, switchMap } from 'rxjs';
import { canonicalId } from '../users/association-identifiers';

export interface EmployeeProfile {
  employeeId: string; firstName: string | null; lastName: string | null;
  jobTitle: string | null; departmentId: string | null; departmentName: string | null; hireDate: string | null;
}
@Injectable({ providedIn: 'root' })
export class EmployeeProfileService {
  private readonly http = inject(HttpClient);
  read() {
    return this.http.get<unknown>('http://localhost:8080/hr/api/employee-profile/me').pipe(map(value => {
      const row = value as EmployeeProfile | null;
      const fields = ['employeeId', 'firstName', 'lastName', 'jobTitle', 'departmentId', 'hireDate'];
      if (!row || typeof row !== 'object' || Object.keys(row).length !== fields.length
        || fields.some(field => !Object.prototype.hasOwnProperty.call(row, field)) || !canonicalId(row.employeeId)
        || (row.departmentId !== null && !canonicalId(row.departmentId))
        || [row.firstName, row.lastName, row.jobTitle].some(text => text !== null && (typeof text !== 'string' || text.length > 500))
        || (row.hireDate !== null && !validDate(row.hireDate))) throw new HttpErrorResponse({ status: 502 });
      return row;
    }), switchMap(row => {
      if (!row.departmentId || !Number.isSafeInteger(Number(row.departmentId))
          || String(Number(row.departmentId)) !== row.departmentId) {
        return of({ ...row, departmentName: null });
      }
      return this.http.get<unknown>(`http://localhost:8080/department/api/departments/${row.departmentId}`).pipe(
        map(value => {
          const department = value as { id?: unknown; name?: unknown } | null;
          return department && typeof department.id === 'number' && Number.isSafeInteger(department.id)
            && String(department.id) === row.departmentId && typeof department.name === 'string'
            && department.name.length <= 255 ? department.name : null;
        }),
        catchError(() => of(null)),
        map(departmentName => ({ ...row, departmentName }))
      );
    }));
  }
}
function validDate(value: unknown): value is string {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(value + 'T00:00:00Z');
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}
