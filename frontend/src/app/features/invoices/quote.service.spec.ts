import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { QuoteService } from './quote.service';

describe('QuoteService', () => {
  const url = 'http://localhost:8080/invoice/api/quotes';
  let service: QuoteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(QuoteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a paged, filtered quote list', () => {
    service.list({ page: 2, size: 25, keyword: ' DEV-1 ', status: 'DRAFT' }).subscribe();
    const request = http.expectOne(req => req.url === url);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('keyword')).toBe('DEV-1');
    expect(request.request.params.get('status')).toBe('DRAFT');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 25 });
  });

  it('retrieves a quote, updates it and converts it with invoice linkage', () => {
    const quote = {
      customerId: '4', issueDate: null, expiryDate: null,
      lines: [{ description: 'Service', quantity: 1, unitPrice: 100 }],
      tax: 0, currency: 'EUR', status: 'DRAFT' as const, notes: null
    };
    service.get(3).subscribe();
    http.expectOne(`${url}/3`).flush({ id: 3, quoteNumber: 'DEV-3', ...quote });
    service.update(3, quote).subscribe();
    const update = http.expectOne(`${url}/3`);
    expect(update.request.method).toBe('PUT');
    update.flush({ id: 3, quoteNumber: 'DEV-3', ...quote });
    service.convert(3).subscribe();
    const convert = http.expectOne(`${url}/3/convert`);
    expect(convert.request.method).toBe('POST');
    convert.flush({ quote: { id: 3, ...quote, convertedInvoiceId: 8 }, invoice: { id: 8, invoiceNumber: 'INV-8' } });
  });
});
