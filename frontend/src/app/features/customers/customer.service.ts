import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CustomerPage, CustomerPageParams, CustomerRequest, CustomerResponse } from './customer.models';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/customer/api/customers';

  list(query: CustomerPageParams) {
    // sortBy is always supplied: the existing API otherwise returns a plain array
    // for its default parameters instead of a Spring Page.
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', query.sortBy).set('direction', query.direction);
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    return this.http.get<CustomerPage>(this.url, { params });
  }
  get(id: number) { return this.http.get<CustomerResponse>(`${this.url}/${id}`); }
  create(request: CustomerRequest) { return this.http.post<CustomerResponse>(this.url, request); }
  update(id: number, request: CustomerRequest) {
    return this.http.put<CustomerResponse>(`${this.url}/${id}`, request);
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
