import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { CustomerPage, CustomerResponse } from '../customers/customer.models';
import { CustomerService } from '../customers/customer.service';
import { InvoiceFormComponent } from './invoice-form.component';
import { InvoiceResponse, parseCustomerId } from './invoice.models';
import { InvoiceService } from './invoice.service';

describe('InvoiceFormComponent', () => {
  const client: CustomerResponse = { id: 2, name: 'Client test', email: null, phone: null, company: null, address: null, createdAt: null, updatedAt: null };
  const page: CustomerPage = { content: [client], totalElements: 1, totalPages: 1, number: 0, size: 25 };
  const row: InvoiceResponse = { id: 4, invoiceNumber: 'FACT-TEST', customerId: '2', issueDate: null,
    dueDate: null, subtotal: 100, tax: null, currency: null, status: 'À traiter', notes: null,
    totalAmount: 100, createdAt: null, updatedAt: null };
  let fixture: ComponentFixture<InvoiceFormComponent>;
  let service: jasmine.SpyObj<InvoiceService>;
  let customers: jasmine.SpyObj<CustomerService>;
  let params: Subject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    params = new Subject(); service = jasmine.createSpyObj('InvoiceService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(row)); service.create.and.returnValue(of(row)); service.update.and.returnValue(of(row));
    customers = jasmine.createSpyObj('CustomerService', ['list', 'get']);
    customers.list.and.returnValue(of(page)); customers.get.and.returnValue(of(client));
    await TestBed.configureTestingModule({ imports: [InvoiceFormComponent], providers: [
      provideRouter([]), provideZonelessChangeDetection(), { provide: InvoiceService, useValue: service },
      { provide: CustomerService, useValue: customers }, { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
    ] }).compileComponents();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(InvoiceFormComponent);
  });
  function start(id?: string) {
    fixture.detectChanges(); params.next(convertToParamMap(id === undefined ? {} : { id })); fixture.detectChanges();
    return fixture.componentInstance;
  }
  function fill(component: InvoiceFormComponent) {
    component['form'].patchValue({ invoiceNumber: ' FACT-TEST ', customerId: 2, subtotal: 100, status: ' À traiter ' });
    component['selectCustomer'](2);
  }
  it('requires invoice number, existing client, positive subtotal and status', () => {
    const c = start(); c['submit'](); expect(service.create).not.toHaveBeenCalled();
    fill(c); expect(c['form'].valid).toBeTrue();
    for (const field of ['invoiceNumber', 'status'] as const) {
      c['form'].controls[field].setValue(' '); expect(c['form'].controls[field].invalid).toBeTrue();
    }
    c['form'].controls.subtotal.setValue(0); expect(c['form'].controls.subtotal.invalid).toBeTrue();
    c['form'].controls.subtotal.setValue(Infinity); expect(c['form'].controls.subtotal.invalid).toBeTrue();
  });
  it('respects DTO lengths and optional dates/tax without inventing status or currency enums', () => {
    const c = start(); fill(c);
    c['form'].patchValue({ status: 'Statut libre', currency: 'LIBRE', tax: -2, issueDate: '', dueDate: '' });
    expect(c['form'].valid).toBeTrue();
    for (const [field, max] of [['invoiceNumber', 50], ['status', 50], ['currency', 10], ['notes', 500]] as const) {
      c['form'].controls[field].setValue('x'.repeat(max + 1)); expect(c['form'].controls[field].hasError('maxlength')).toBeTrue();
    }
    c['form'].controls.issueDate.setValue('2027-02-30'); expect(c['form'].controls.issueDate.invalid).toBeTrue();
  });
  it('converts a selected numeric client to a string and never sends totalAmount', () => {
    const c = start(); fill(c); c['submit']();
    expect(service.create).toHaveBeenCalledOnceWith({ invoiceNumber: 'FACT-TEST', customerId: '2',
      issueDate: null, dueDate: null, subtotal: 100, tax: null, currency: null, status: 'À traiter', notes: null });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/invoices']);
  });
  it('rejects unknown, fractional and unsafe selected IDs', () => {
    const c = start(); fill(c);
    for (const id of [999, 2.5, Number.MAX_SAFE_INTEGER + 1]) {
      c['form'].controls.customerId.setValue(id); c['submit']();
    }
    expect(service.create).not.toHaveBeenCalled();
    for (const raw of ['abc', '2.5', '02', '0', '9007199254740992']) expect(parseCustomerId(raw)).toBeNull();
    expect(parseCustomerId('2')).toBe(2);
  });
  it('shows no-client state and offers Clients without creating anything', () => {
    customers.list.and.returnValue(of({ ...page, content: [], totalElements: 0, totalPages: 0 }));
    const c = start(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun client disponible');
    expect(fixture.nativeElement.querySelector('a[href="/customers"]')).toBeTruthy();
    fill(c); c['submit'](); expect(service.create).not.toHaveBeenCalled();
  });
  it('reports directory errors and blocks submission until the directory recovers', () => {
    customers.list.and.returnValues(throwError(() => ({ status: 503 })), of(page));
    const c = start(); fill(c); c['submit'](); expect(service.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Impossible de charger les clients');
    c['loadCustomers'](); fill(c); c['submit'](); expect(service.create).toHaveBeenCalledTimes(1);
  });
  it('searches and paginates clients on the server while preserving selection', () => {
    customers.list.and.returnValues(of({ ...page, totalPages: 2, totalElements: 30 }), of({ ...page, content: [], number: 1, totalPages: 2 }), of(page));
    const c = start(); fill(c); c['changeCustomerPage'](1);
    expect(customers.list.calls.mostRecent().args[0].page).toBe(1);
    expect(c['pinnedCustomer']()?.id).toBe(2); expect(c['form'].controls.customerId.valid).toBeTrue();
    c['customerSearch'].setValue(' Test '); c['searchCustomers']();
    expect(customers.list.calls.mostRecent().args[0].page).toBe(0);
    expect(customers.list.calls.mostRecent().args[0].keyword).toBe('Test');
  });
  it('prefills an existing client even outside the loaded page and updates', () => {
    const other = { ...client, id: 7, name: 'Autre client' };
    customers.list.and.returnValue(of({ ...page, content: [other] }));
    const c = start('4'); expect(customers.get).toHaveBeenCalledOnceWith(2);
    expect(c['pinnedCustomer']()?.id).toBe(2); expect(c['form'].controls.customerId.value).toBe(2);
    c['form'].controls.notes.setValue(' Note '); c['submit']();
    expect(service.update).toHaveBeenCalledWith(4, { ...rowRequest(), notes: 'Note' });
    expect(service.create).not.toHaveBeenCalled();
  });
  function rowRequest() {
    return { invoiceNumber: row.invoiceNumber, customerId: row.customerId, issueDate: null, dueDate: null,
      subtotal: row.subtotal, tax: null, currency: null, status: row.status, notes: null };
  }
  it('handles invalid legacy or missing customer references without silently converting them', () => {
    service.get.and.returnValue(of({ ...row, customerId: 'legacy' }));
    const c = start('4'); expect(customers.get).not.toHaveBeenCalled(); c['submit']();
    expect(service.update).not.toHaveBeenCalled(); expect(c['customerNotice']()).toContain('référence client');
    service.get.and.returnValue(of(row)); customers.get.and.returnValue(throwError(() => ({ status: 404 })));
    params.next(convertToParamMap({ id: '4' })); expect(c['form'].controls.customerId.value).toBeNull();
  });
  it('prevents double submissions', () => {
    const pending = new Subject<InvoiceResponse>(); service.create.and.returnValue(pending);
    const c = start(); fill(c); c['submit'](); c['submit'](); expect(service.create).toHaveBeenCalledTimes(1);
    pending.next(row); pending.complete();
  });
  it('maps duplicate/validation errors to French messages without exposing raw backend text', () => {
    service.create.and.returnValue(throwError(() => ({ status: 400, error: { message: 'Invoice number already exists: PRIVATE' } })));
    const c = start(); fill(c); c['submit'](); fixture.detectChanges();
    expect(c['form'].controls.invoiceNumber.hasError('server')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement refusé');
    expect(fixture.nativeElement.textContent).not.toContain('PRIVATE');
  });
  it('blocks writes for invalid routes and failed invoice reads', () => {
    const c = start('invalid'); c['submit'](); expect(service.get).not.toHaveBeenCalled();
    service.get.and.returnValue(throwError(() => ({ status: 404 })));
    params.next(convertToParamMap({ id: '4' })); c['submit'](); expect(service.update).not.toHaveBeenCalled();
    expect(c['error']()).toContain('Facture introuvable');
  });
  it('shows a recoverable save error', () => {
    service.create.and.returnValue(throwError(() => ({ status: 503 })));
    const c = start(); fill(c); c['submit'](); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Impossible d’enregistrer');
    expect(c['saving']()).toBeFalse();
  });
});
