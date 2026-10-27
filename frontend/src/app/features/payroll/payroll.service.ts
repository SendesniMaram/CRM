import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PayrollPage, PayrollPageParams, PayrollRequest, PayrollResponse } from './payroll.models';
@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/payroll/api/payrolls';
  list(query: PayrollPageParams) {
    // Supplying sortBy ensures the existing controller returns a Spring Page.
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', query.sortBy).set('direction', query.direction);
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    return this.http.get<PayrollPage>(this.url, { params });
  }
  get(id: number) { return this.http.get<PayrollResponse>(`${this.url}/${id}`); }
  create(value: PayrollRequest) { return this.http.post<PayrollResponse>(this.url, value); }
  update(id: number, value: PayrollRequest) { return this.http.put<PayrollResponse>(`${this.url}/${id}`, value); }
  setPublished(id: number, published: boolean) {
    return this.http.patch<PayrollResponse>(`${this.url}/${id}/publication`, { published });
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
