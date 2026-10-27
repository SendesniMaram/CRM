import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { CrmRole } from '../../core/models/role.model';
import { RegisterRequest } from '../../core/models/register.models';
import { canonicalId, parseDirectory } from './association-identifiers';

export interface DirectoryUser {
  id: string; firstName: string | null; lastName: string | null; username: string; email: string;
  phone: string | null; enabled: boolean; accountNonLocked: boolean;
  roles: CrmRole[]; roleType: CrmRole | null; createdAt: string | null;
}
export type AdminRole = CrmRole;

@Injectable({ providedIn: 'root' })
export class UserDirectoryService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/identity/api/users';
  list() {
    return this.http.get(this.url, { responseType: 'text' }).pipe(map(text => {
      const rows: DirectoryUser[] = parseDirectory(text);
      if (!Array.isArray(rows) || new Set(rows.map(row => row?.id)).size !== rows.length || rows.some(row => !row
        || !canonicalId(row.id) || typeof row.username !== 'string'
        || typeof row.email !== 'string' || typeof row.enabled !== 'boolean' || typeof row.accountNonLocked !== 'boolean'
        || !Array.isArray(row.roles) || row.roles.some(role => !Object.values(CrmRole).includes(role)))) {
        throw new HttpErrorResponse({ status: 502 });
      }
      return rows;
    }));
  }
  createEmployee(request: RegisterRequest) {
    // Whitelist the existing register DTO. Roles and account flags are never caller-selected.
    const { firstName, lastName, username, email, password, phone } = request;
    return this.http.post<void>(this.url, { firstName, lastName, username, email, password, phone }, { observe: 'response' })
      .pipe(map(response => {
        if (response.status !== 201) throw new HttpErrorResponse({ status: 502 });
      }));
  }
  create(request: RegisterRequest & { roleType: AdminRole }) {
    return this.http.post<DirectoryUser>(`${this.url}/admin`, request);
  }
  update(id: string, request: Pick<DirectoryUser, 'firstName' | 'lastName' | 'email' | 'phone'>) {
    return this.http.put<DirectoryUser>(`${this.url}/${encodeURIComponent(id)}`, request);
  }
  setRoles(id: string, roles: AdminRole[]) {
    return this.http.put<DirectoryUser>(`${this.url}/${encodeURIComponent(id)}/roles`, { roles });
  }
  setEnabled(id: string, enabled: boolean) {
    return this.http.patch<DirectoryUser>(`${this.url}/${encodeURIComponent(id)}/status`, { enabled });
  }
}
