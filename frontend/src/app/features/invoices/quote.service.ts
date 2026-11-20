import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { QuotePage, QuoteRequest, QuoteResponse } from './quote.models';

export interface QuoteQuery {
  page: number;
  size: number;
  keyword?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class QuoteService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/invoice/api/quotes';

  list(query: QuoteQuery) {
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', 'createdAt').set('direction', 'desc');
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    if (query.status) params = params.set('status', query.status);
    return this.http.get<QuotePage>(this.url, { params });
  }
  get(id: number) { return this.http.get<QuoteResponse>(`${this.url}/${id}`); }
  create(value: QuoteRequest) { return this.http.post<QuoteResponse>(this.url, value); }
  update(id: number, value: QuoteRequest) { return this.http.put<QuoteResponse>(`${this.url}/${id}`, value); }
  status(id: number, status: string) { return this.http.patch<QuoteResponse>(`${this.url}/${id}/status`, { status }); }
  convert(id: number) {
    return this.http.post<{ quote: QuoteResponse; invoice: { id: number; invoiceNumber: string } }>(
      `${this.url}/${id}/convert`, {}
    );
  }
  pdf(id: number) { return this.http.get(`${this.url}/${id}/pdf`, { responseType: 'blob' }); }
}
