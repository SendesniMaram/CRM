import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { CustomerFormComponent } from './customer-form.component';
import { CustomerResponse } from './customer.models';
import { CustomerService } from './customer.service';

describe('CustomerFormComponent', () => {
  const row: CustomerResponse = { id: 4, name: 'Client test', email: null, company: null, phone: null,
    address: null, createdAt: null, updatedAt: null };
  let fixture: ComponentFixture<CustomerFormComponent>;
  let service: jasmine.SpyObj<CustomerService>;
  let params: Subject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    params = new Subject(); service = jasmine.createSpyObj('CustomerService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(row)); service.create.and.returnValue(of(row)); service.update.and.returnValue(of(row));
    await TestBed.configureTestingModule({ imports: [CustomerFormComponent], providers: [
      provideRouter([]), provideZonelessChangeDetection(), { provide: CustomerService, useValue: service },
      { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
    ] }).compileComponents();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(CustomerFormComponent); fixture.detectChanges();
  });
  function start(id?: string) {
    params.next(convertToParamMap(id === undefined ? {} : { id })); fixture.detectChanges();
    return fixture.componentInstance;
  }
  it('requires a nonblank name and allows empty optional fields', () => {
    const component = start(); component['submit'](); expect(service.create).not.toHaveBeenCalled();
    component['form'].controls.name.setValue(' '); expect(component['form'].invalid).toBeTrue();
    component['form'].controls.name.setValue('Client'); expect(component['form'].valid).toBeTrue();
  });
  it('validates email, phone and backend maximum lengths', () => {
    const component = start(); const form = component['form'];
    form.patchValue({ name: 'Client', email: 'invalid', phone: 'abc' });
    expect(form.controls.email.hasError('email')).toBeTrue(); expect(form.controls.phone.hasError('pattern')).toBeTrue();
    form.patchValue({ email: ' test@example.test ', phone: ' +21612345678 ' }); expect(form.valid).toBeTrue();
    for (const [field, max] of [['name', 255], ['email', 255], ['phone', 50], ['company', 255], ['address', 500]] as const) {
      form.controls[field].setValue('x'.repeat(max + 1)); expect(form.controls[field].hasError('maxlength')).toBeTrue();
    }
  });
  it('creates a trimmed request with null optional fields and returns to the list', () => {
    const component = start(); component['form'].patchValue({ name: ' Client ', email: ' ', company: ' Société ' });
    component['submit']();
    expect(service.create).toHaveBeenCalledOnceWith({ name: 'Client', email: null, phone: null, company: 'Société', address: null });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/customers']);
  });
  it('prefills and updates an existing customer', () => {
    const component = start('4'); expect(component['form'].controls.name.value).toBe(row.name);
    component['form'].controls.name.setValue('Modifié'); component['submit']();
    expect(service.update).toHaveBeenCalledOnceWith(4, { name: 'Modifié', email: null, phone: null, company: null, address: null });
    expect(service.create).not.toHaveBeenCalled();
  });
  it('prevents duplicate submission while saving', () => {
    const pending = new Subject<CustomerResponse>(); service.create.and.returnValue(pending);
    const component = start(); component['form'].controls.name.setValue('Client');
    component['submit'](); component['submit'](); expect(service.create).toHaveBeenCalledTimes(1);
    pending.next(row); pending.complete();
  });
  it('shows translated server field validation and allows a corrected retry', () => {
    service.create.and.returnValues(throwError(() => ({ status: 400, error: { message: 'Validation failed: email: Email must be valid' } })), of(row));
    const component = start(); component['form'].patchValue({ name: 'Client', email: 'first@example.test' }); component['submit']();
    fixture.detectChanges(); expect(component['form'].controls.email.hasError('server')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement refusé');
    component['form'].controls.email.setValue('new@example.test'); component['submit']();
    expect(service.create).toHaveBeenCalledTimes(2);
  });
  it('maps duplicate fields without reflecting raw database messages', () => {
    service.create.and.returnValue(throwError(() => ({ status: 400, error: { message: 'Customer phone already exists: private' } })));
    const component = start(); component['form'].controls.name.setValue('Client'); component['submit'](); fixture.detectChanges();
    expect(component['form'].controls.phone.hasError('server')).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('private');
  });
  it('does not submit invalid IDs or failed detail loads', () => {
    const component = start('invalid'); component['submit'](); expect(service.get).not.toHaveBeenCalled();
    service.get.and.returnValue(throwError(() => ({ status: 404 }))); start('4'); component['submit'](); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Client introuvable');
    expect(service.update).not.toHaveBeenCalled();
  });
  it('shows a recoverable API error and keeps the form available', () => {
    service.create.and.returnValue(throwError(() => ({ status: 503 })));
    const component = start(); component['form'].controls.name.setValue('Client'); component['submit'](); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible d’enregistrer');
    expect(component['saving']()).toBeFalse(); expect(component['ready']()).toBeTrue();
  });
});
