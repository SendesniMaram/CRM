import { HttpClient, HttpContext, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { PDF_DOWNLOAD_REQUEST } from '../../core/http/pdf-download-context';
import { PayrollPage } from './payroll.models';
import { PayrollResponse } from './payroll.models';

@Injectable({ providedIn: 'root' })
export class MyPayrollService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/payroll/api/payrolls/me';

  list(page = 0, period = '') {
    if (!Number.isSafeInteger(page) || page < 0) throw new Error('Invalid page');
    let params = new HttpParams().set('page', page).set('size', 10);
    if (period) params = params.set('period', period);
    return this.http.get<PayrollPage>(this.url, { params }).pipe(map(result => {
      if (!result || !Array.isArray(result.content) || result.number !== page
        || !Number.isSafeInteger(result.totalElements) || result.totalElements < 0
        || !Number.isSafeInteger(result.totalPages) || result.totalPages < 0
        || result.content.some(row => !validPayroll(row))) throw new HttpErrorResponse({ status: 502 });
      return result;
    }));
  }

  read(id: string) {
    this.requireId(id);
    return this.http.get<PayrollResponse>(`${this.url}/${id}`).pipe(map(row => {
      if (!validPayroll(row) || String(row.id) !== id) throw new HttpErrorResponse({ status: 502 });
      return row;
    }));
  }

  pdf(id: string) {
    this.requireId(id);
    return this.http.get(`${this.url}/${id}/pdf`, {
      responseType: 'blob', observe: 'response', context: new HttpContext().set(PDF_DOWNLOAD_REQUEST, true)
    }).pipe(map(response => {
      if (!response.body?.size || response.headers.get('Content-Type')?.split(';')[0] !== 'application/pdf') {
        throw new HttpErrorResponse({ status: 502 });
      }
      return response.body;
    }));
  }

  private requireId(id: string) {
    if (!/^[1-9][0-9]{0,18}$/.test(id)) throw new Error('Invalid payroll id');
  }
}

function validPayroll(row: PayrollResponse): boolean {
  return !!row && Number.isSafeInteger(row.id) && row.id > 0 && typeof row.employeeId === 'string'
    && typeof row.payPeriod === 'string' && typeof row.status === 'string'
    && typeof row.baseSalary === 'number' && Number.isFinite(row.baseSalary)
    && typeof row.netSalary === 'number' && Number.isFinite(row.netSalary)
    && (row.currency === null || typeof row.currency === 'string');
}
