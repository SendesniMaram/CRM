import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EmployeePage,
  EmployeePageParams,
  EmployeeRequest,
  EmployeeResponse,
  EmployeeWithDepartmentResponse
} from '../models/employee.models';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly employeesUrl = 'http://localhost:8080/employee/api/employees';

  getEmployees(): Observable<EmployeeResponse[]> {
    return this.http.get<EmployeeResponse[]>(this.employeesUrl);
  }

  getEmployeesPage(params: EmployeePageParams): Observable<EmployeePage> {
    return this.http.get<EmployeePage>(this.employeesUrl, {
      params: this.toHttpParams(params)
    });
  }

  getEmployeeById(id: number): Observable<EmployeeResponse> {
    return this.http.get<EmployeeResponse>(`${this.employeesUrl}/${id}`);
  }

  getEmployeeDetails(id: number): Observable<EmployeeWithDepartmentResponse> {
    return this.http.get<EmployeeWithDepartmentResponse>(`${this.employeesUrl}/${id}/details`);
  }

  createEmployee(employee: EmployeeRequest): Observable<EmployeeResponse> {
    return this.http.post<EmployeeResponse>(this.employeesUrl, employee);
  }

  updateEmployee(id: number, employee: EmployeeRequest): Observable<EmployeeResponse> {
    return this.http.put<EmployeeResponse>(`${this.employeesUrl}/${id}`, employee);
  }

  deleteEmployee(id: number): Observable<void> {
    return this.http.delete<void>(`${this.employeesUrl}/${id}`);
  }

  private toHttpParams(params: EmployeePageParams): HttpParams {
    let httpParams = new HttpParams();

    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page);
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size);
    }
    if (params.sortBy !== undefined) {
      httpParams = httpParams.set('sortBy', params.sortBy);
    }
    if (params.direction !== undefined) {
      httpParams = httpParams.set('direction', params.direction);
    }
    if (params.keyword !== undefined) {
      httpParams = httpParams.set('keyword', params.keyword);
    }

    return httpParams;
  }
}
