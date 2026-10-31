import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CustomerService } from '../customers/customer.service';
import { QuoteResponse } from './quote.models';
import { QuoteService } from './quote.service';
import { QuotesPageComponent } from './quotes-page.component';

describe('QuotesPageComponent', () => {
  let fixture: ComponentFixture<QuotesPageComponent>;
  let quotes: jasmine.SpyObj<QuoteService>;
  let customers: jasmine.SpyObj<CustomerService>;

  beforeEach(async () => {
    quotes = jasmine.createSpyObj<QuoteService>('QuoteService', ['list', 'create', 'update', 'get', 'status', 'convert', 'pdf']);
    customers = jasmine.createSpyObj<CustomerService>('CustomerService', ['list', 'get']);
    quotes.list.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }));
    customers.list.and.returnValue(of({ content: [
      { id: 5, name: 'Client', email: null, phone: null, company: null, address: null, contactType: 'CUSTOMER',
        category: null, segment: null, assignedEmployeeId: null, createdAt: null, updatedAt: null }
    ], totalElements: 1, totalPages: 1, number: 0, size: 25 }));
    const created: QuoteResponse = {
      id: 11, quoteNumber: 'DEV-TEST', customerId: '5', issueDate: null, expiryDate: null,
      lines: [{ description: 'Service', quantity: 2, unitPrice: 12.5, lineTotal: 25 }],
      subtotal: 25, tax: 0, totalAmount: 25, currency: 'EUR', status: 'DRAFT',
      convertedInvoiceId: null, notes: null, createdAt: null, updatedAt: null
    };
    quotes.create.and.returnValue(of(created));
    quotes.update.and.returnValue(of(created));
    quotes.get.and.returnValue(of(created));
    quotes.status.and.returnValue(of(created));
    quotes.convert.and.returnValue(of({ quote: created, invoice: { id: 12, invoiceNumber: 'INV-12' } }));
    await TestBed.configureTestingModule({
      imports: [QuotesPageComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: QuoteService, useValue: quotes },
        { provide: CustomerService, useValue: customers },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(QuotesPageComponent);
    fixture.detectChanges();
  });

  it('creates a draft with its selected client and calculated lines', () => {
    const component = fixture.componentInstance as unknown as {
      form: {
        patchValue(value: { customerId: string; tax: number; currency: string }): void;
        controls: { lines: { at(index: number): { patchValue(value: object): void } } };
      };
      save(): void;
    };
    component.form.patchValue({ customerId: '5', tax: 0, currency: 'EUR' });
    component.form.controls.lines.at(0).patchValue({ description: ' Service ', quantity: 2, unitPrice: 12.5 });
    component.save();
    expect(quotes.create).toHaveBeenCalledOnceWith(jasmine.objectContaining({
      customerId: '5',
      status: 'DRAFT',
      lines: [{ description: 'Service', quantity: 2, unitPrice: 12.5 }],
      tax: 0
    }));
    expect(fixture.nativeElement.textContent).toContain('Nouveau devis');
  });

  it('does not send an incomplete quote', () => {
    (fixture.componentInstance as unknown as { save(): void }).save();
    expect(quotes.create).not.toHaveBeenCalled();
  });
});
