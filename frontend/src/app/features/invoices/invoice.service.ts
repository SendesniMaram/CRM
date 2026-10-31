import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { InvoicePage, InvoicePageParams, InvoiceRequest, InvoiceResponse, InvoicePaymentSummary, InvoicePayment, InvoiceReminder } from './invoice.models';
@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/invoice/api/invoices';
  list(query: InvoicePageParams) {
    // Supplying sortBy ensures the existing controller returns a Spring Page.
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', query.sortBy).set('direction', query.direction);
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    return this.http.get<InvoicePage>(this.url, { params });
  }
  get(id: number) { return this.http.get<InvoiceResponse>(`${this.url}/${id}`); }
  create(value: InvoiceRequest) { return this.http.post<InvoiceResponse>(this.url, value); }
  update(id: number, value: InvoiceRequest) { return this.http.put<InvoiceResponse>(`${this.url}/${id}`, value); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
  payments(id: number) { return this.http.get<InvoicePaymentSummary>(`${this.url}/${id}/payments`); }
  recordPayment(id: number, value: { amount: number; paidAt: string; method: string | null; reference: string | null; notes: string | null }) { return this.http.post<InvoicePayment>(`${this.url}/${id}/payments`, value); }
  reminders(id: number) { return this.http.get<InvoiceReminder[]>(`${this.url}/${id}/reminders`); }
  recordReminder(id: number, value: { channel: string; notes: string | null }) { return this.http.post<InvoiceReminder>(`${this.url}/${id}/reminders`, value); }
}
