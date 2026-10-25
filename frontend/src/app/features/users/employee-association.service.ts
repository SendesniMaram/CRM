import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { defer, map, throwError } from 'rxjs';
import { canonicalId, parseDirectory } from './association-identifiers';

export interface EmployeeAssociation { userId: string; employeeId: string; }
export interface AssociationOption { id: string; label: string; }
export interface AssociationOptions { users: AssociationOption[]; employees: AssociationOption[]; }

@Injectable({ providedIn: 'root' })
export class EmployeeAssociationService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/hr/api/employee-associations';
  options() {
    return this.http.get('http://localhost:8080/hr/api/leaves/associations/options', { responseType: 'text' }).pipe(map(text => {
      const value = parseDirectory(text);
      for (const key of ['users', 'employees']) {
        const rows = value?.[key];
        if (!Array.isArray(rows) || rows.some(row => !row || !canonicalId(row.id) || typeof row.label !== 'string' || !row.label.trim())
          || new Set(rows.map(row => row.id)).size !== rows.length) throw new HttpErrorResponse({ status: 502 });
      }
      return value as AssociationOptions;
    }));
  }
  read(userId: string) {
    return defer(() => canonicalId(userId) ? this.http.get<unknown>(`${this.url}/users/${userId}`).pipe(
      map(body => this.validate(body, userId))) : throwError(() => new HttpErrorResponse({ status: 400 })));
  }
  create(userId: string, employeeId: string) {
    return defer(() => canonicalId(userId) && canonicalId(employeeId)
      ? this.http.post<unknown>(this.url, { userId, employeeId }, { observe: 'response' }).pipe(map(response => {
        if (response.status !== 200 && response.status !== 201) throw new HttpErrorResponse({ status: 502 });
        const association = this.validate(response.body, userId);
        if (association.employeeId !== employeeId) throw new HttpErrorResponse({ status: 502 });
        return { association, created: response.status === 201 };
      })) : throwError(() => new HttpErrorResponse({ status: 400 })));
  }
  private validate(body: unknown, userId: string): EmployeeAssociation {
    const link = body as EmployeeAssociation | null;
    if (!link || !canonicalId(link.userId) || !canonicalId(link.employeeId) || link.userId !== userId) {
      throw new HttpErrorResponse({ status: 502 });
    }
    return { userId: link.userId, employeeId: link.employeeId };
  }
}
