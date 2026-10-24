import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { canonicalId } from '../users/association-identifiers';

export interface EmployeeProfile {
  employeeId: string; firstName: string | null; lastName: string | null;
  jobTitle: string | null; departmentId: string | null; hireDate: string | null;
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
    }));
  }
}
function validDate(value: unknown): value is string {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(value + 'T00:00:00Z');
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}
