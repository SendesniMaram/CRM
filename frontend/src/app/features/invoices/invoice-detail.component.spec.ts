import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { InvoiceDetailComponent } from './invoice-detail.component';
import { InvoiceService } from './invoice.service';
describe('InvoiceDetailComponent', () => {
  async function create(id: string, fails = false) {
    const service = jasmine.createSpyObj<InvoiceService>('InvoiceService', ['get', 'payments', 'reminders', 'recordPayment', 'recordReminder']);
    service.get.and.returnValue(fails ? throwError(() => ({ status: 404 })) : of({
      id: 4, invoiceNumber: 'FACT-TEST', customerId: '2', issueDate: '2026-10-14', dueDate: null,
      subtotal: 100, tax: 10, totalAmount: 123.45, currency: 'TND', status: 'Statut libre',
      notes: null, createdAt: null, updatedAt: null
    }));
    service.payments.and.returnValue(of({ payments: [], paidAmount: 0, outstandingAmount: 123.45 }));
    service.reminders.and.returnValue(of([]));
    await TestBed.configureTestingModule({ imports: [InvoiceDetailComponent], providers: [provideRouter([]),
      provideZonelessChangeDetection(), { provide: InvoiceService, useValue: service },
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } }] }).compileComponents();
    const fixture = TestBed.createComponent(InvoiceDetailComponent); fixture.detectChanges(); return { fixture, service };
  }
  it('shows server total, French amounts/dates and the edit link', async () => {
    const { fixture } = await create('4');
    expect(fixture.nativeElement.textContent).toContain('123,45');
    expect(fixture.nativeElement.textContent).toContain('14/10/2026');
    expect(fixture.nativeElement.textContent).toContain('Statut libre');
    expect(fixture.nativeElement.querySelector('a[href="/invoices/4/edit"]')).toBeTruthy();
  });
  it('does not request an invalid ID', async () => {
    const { fixture, service } = await create('invalid');
    expect(service.get).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Facture introuvable');
  });
  it('shows a missing invoice without losing back navigation', async () => {
    const { fixture } = await create('4', true);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Facture introuvable');
    expect(fixture.nativeElement.querySelector('a[href="/invoices"]')).toBeTruthy();
  });
});
