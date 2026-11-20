import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DepartmentResponse } from '../../employees/models/employee.models';

export interface DepartmentRequest {
  name: string;
  code?: string | null;
  description?: string | null;
}

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/department/api/departments';

  getDepartments(): Observable<DepartmentResponse[]> {
    return this.http.get<DepartmentResponse[]>(this.url);
  }

  getDepartmentById(id: number): Observable<DepartmentResponse> {
    return this.http.get<DepartmentResponse>(`${this.url}/${id}`);
  }

  createDepartment(request: DepartmentRequest): Observable<DepartmentResponse> {
    return this.http.post<DepartmentResponse>(this.url, request);
  }

  updateDepartment(id: number, request: DepartmentRequest): Observable<DepartmentResponse> {
    return this.http.put<DepartmentResponse>(`${this.url}/${id}`, request);
  }

  deleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
