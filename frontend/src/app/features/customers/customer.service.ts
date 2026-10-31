import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  CustomerInteraction,
  CustomerInteractionRequest,
  CustomerPage,
  CustomerPageParams,
  CustomerCsvImportReport,
  CustomerRequest,
  CustomerResponse
} from './customer.models';

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
    if (query.contactType) params = params.set('contactType', query.contactType);
    if (query.category?.trim()) params = params.set('category', query.category.trim());
    if (query.segment?.trim()) params = params.set('segment', query.segment.trim());
    return this.http.get<CustomerPage>(this.url, { params });
  }
  get(id: number) { return this.http.get<CustomerResponse>(`${this.url}/${id}`); }
  create(request: CustomerRequest) { return this.http.post<CustomerResponse>(this.url, request); }
  update(id: number, request: CustomerRequest) {
    return this.http.put<CustomerResponse>(`${this.url}/${id}`, request);
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
  convertLead(id: number) { return this.http.post<CustomerResponse>(`${this.url}/${id}/convert`, {}); }
  listInteractions(customerId: number) {
    return this.http.get<CustomerInteraction[]>(`${this.url}/${customerId}/interactions`);
  }
  addInteraction(customerId: number, request: CustomerInteractionRequest) {
    return this.http.post<CustomerInteraction>(`${this.url}/${customerId}/interactions`, request);
  }
  importCsv(file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<CustomerCsvImportReport>(`${this.url}/import`, form);
  }
  exportCsv(query: Pick<CustomerPageParams, 'keyword' | 'contactType' | 'category' | 'segment'>) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value?.trim()) params = params.set(key, value.trim());
    }
    return this.http.get(`${this.url}/export`, { params, responseType: 'blob' });
  }
  exportExcel(query: Pick<CustomerPageParams, 'keyword' | 'contactType' | 'category' | 'segment'>) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value?.trim()) params = params.set(key, value.trim());
    }
    return this.http.get(`${this.url}/export.xlsx`, { params, responseType: 'blob' });
  }
}
