import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CustomerService } from '../customers/customer.service';
import { EmployeeService } from '../employees/services/employee.service';
import { FeeFormComponent } from './fee-form.component';
import { FeeRequest, FeeResponse } from './fee.models';
import { FeeService } from './fee.service';

describe('FeeFormComponent', () => {
  const request: FeeRequest = { employeeId: 'EMP-legacy', customerId: null, serviceName: 'Conseil test',
    description: null, hourlyRate: 50, workedHours: 2.5, invoiceStatus: null, paymentStatus: null, workDate: null };
  // Deliberately differs from rate * hours: the UI must trust the server amount.
  const row: FeeResponse = { id: 4, ...request, amount: 123.45, createdAt: null, updatedAt: null };
  let fixture: ComponentFixture<FeeFormComponent>;
  let service: jasmine.SpyObj<FeeService>;
  let auth: jasmine.SpyObj<AuthService>;
  let customers: jasmine.SpyObj<CustomerService>;
  let employees: jasmine.SpyObj<EmployeeService>;
  let params: Subject<ReturnType<typeof convertToParamMap>>;

  beforeEach(async () => {
    params = new Subject(); service = jasmine.createSpyObj('FeeService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(row)); service.create.and.returnValue(of(row)); service.update.and.returnValue(of(row));
    auth = jasmine.createSpyObj('AuthService', ['hasAnyRole']); auth.hasAnyRole.and.returnValue(true);
    customers = jasmine.createSpyObj('CustomerService', ['list']);
    employees = jasmine.createSpyObj('EmployeeService', ['getEmployeesPage']);
    await TestBed.configureTestingModule({ imports: [FeeFormComponent], providers: [provideRouter([]),
      provideZonelessChangeDetection(), { provide: FeeService, useValue: service },
      { provide: AuthService, useValue: auth }, { provide: CustomerService, useValue: customers },
      { provide: EmployeeService, useValue: employees }, { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
    ] }).compileComponents();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(FeeFormComponent); fixture.detectChanges();
  });
  function start(id?: string) {
    params.next(convertToParamMap(id === undefined ? {} : { id })); fixture.detectChanges();
    return fixture.componentInstance;
  }
  function fill(c: FeeFormComponent) {
    c['form'].patchValue({ employeeId: ' EMP-legacy ', serviceName: ' Conseil test ', hourlyRate: 50, workedHours: 2.5 });
  }
  it('requires employee, service, positive finite rate and hours', () => {
    const c = start(); c['submit'](); expect(service.create).not.toHaveBeenCalled(); fill(c);
    expect(c['form'].valid).toBeTrue();
    for (const key of ['employeeId', 'serviceName'] as const) {
      c['form'].controls[key].setValue(' '); expect(c['form'].controls[key].invalid).toBeTrue();
    }
    for (const key of ['hourlyRate', 'workedHours'] as const) {
      for (const value of [null, 0, -1, Infinity, NaN]) {
        c['form'].controls[key].setValue(value); expect(c['form'].controls[key].invalid).toBeTrue();
      }
    }
  });
  it('enforces DTO lengths and real calendar dates without fixed status enums', () => {
    const c = start(); fill(c);
    c['form'].patchValue({ invoiceStatus: 'Statut libre', paymentStatus: 'Autre statut', workDate: '2028-02-29' });
    expect(c['form'].valid).toBeTrue();
    for (const value of ['2027-02-29', '2026-04-31', '14/10/2026', 'invalid']) {
      c['form'].controls.workDate.setValue(value); expect(c['form'].controls.workDate.invalid).toBeTrue();
    }
    c['form'].controls.workDate.setValue(''); expect(c['form'].controls.workDate.valid).toBeTrue();
    for (const [key, max] of [['employeeId', 50], ['customerId', 50], ['serviceName', 255],
      ['description', 500], ['invoiceStatus', 50], ['paymentStatus', 50]] as const) {
      c['form'].controls[key].setValue('x'.repeat(max)); expect(c['form'].controls[key].valid).toBeTrue();
      c['form'].controls[key].setValue('x'.repeat(max + 1)); expect(c['form'].controls[key].hasError('maxlength')).toBeTrue();
    }
  });
  it('creates trimmed DTO fields and omits server-calculated amount and timestamps', () => {
    const c = start(); fill(c); c['form'].controls.customerId.setValue(' '); c['submit']();
    expect(service.create).toHaveBeenCalledOnceWith(request);
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/fees']);
    expect(TestBed.inject(MatSnackBar).open).toHaveBeenCalledWith('Honoraire créé.', 'Fermer', { duration: 5000 });
    expect(employees.getEmployeesPage).not.toHaveBeenCalled(); expect(customers.list).not.toHaveBeenCalled();
  });
  it('preserves arbitrary existing string references on edit without mandatory directory access', () => {
    service.get.and.returnValue(of({ ...row, customerId: 'CLIENT-legacy' }));
    const c = start('4'); expect(c['form'].controls.employeeId.value).toBe('EMP-legacy'); c['submit']();
    expect(service.update).toHaveBeenCalledOnceWith(4, { ...request, customerId: 'CLIENT-legacy' });
    expect(service.create).not.toHaveBeenCalled();
  });
  it('accepts string references emitted by directory assistance without numeric coercion', () => {
    const c = start(); fill(c); c['selectReference']('employee', '7'); c['selectReference']('customer', '2'); c['submit']();
    expect(service.create).toHaveBeenCalledOnceWith({ ...request, employeeId: '7', customerId: '2' });
    expect(c['lookup']()).toBeNull();
  });
  it('prevents duplicate submissions and reference changes during save', () => {
    const pending = new Subject<FeeResponse>(); service.create.and.returnValue(pending);
    const c = start(); fill(c); c['submit'](); c['submit'](); c['selectReference']('employee', '8');
    expect(service.create).toHaveBeenCalledTimes(1); expect(c['form'].controls.employeeId.value).toBe(' EMP-legacy ');
    pending.next(row); pending.complete(); expect(c['saving']()).toBeFalse();
  });
  it('denies writes after administrative permission is lost', () => {
    const c = start(); fill(c); auth.hasAnyRole.and.returnValue(false); c['submit']();
    expect(service.create).not.toHaveBeenCalled();
  });
  it('does not write for invalid routes, missing records or reads still loading', () => {
    const c = start('invalid'); fill(c); c['submit'](); expect(service.get).not.toHaveBeenCalled();
    service.get.and.returnValue(throwError(() => ({ status: 404 })));
    params.next(convertToParamMap({ id: '4' })); c['submit'](); expect(c['error']()).toContain('introuvable');
    service.get.and.returnValue(new Subject<FeeResponse>()); params.next(convertToParamMap({ id: '5' }));
    fill(c); c['submit'](); expect(service.update).not.toHaveBeenCalled();
  });
  it('maps server validation without exposing raw messages and allows retry', () => {
    service.create.and.returnValues(throwError(() => ({ status: 400, error: { message: 'hourlyRate: PRIVATE SQL' } })), of(row));
    const c = start(); fill(c); c['submit'](); fixture.detectChanges();
    expect(c['form'].controls.hourlyRate.hasError('server')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement refusé');
    expect(fixture.nativeElement.textContent).not.toContain('PRIVATE');
    c['submit'](); expect(service.create).toHaveBeenCalledTimes(2);
  });
  for (const status of [409, 503, 0]) {
    it(`shows a recoverable French save error for ${status}`, () => {
      service.create.and.returnValue(throwError(() => ({ status })));
      const c = start(); fill(c); c['submit'](); fixture.detectChanges();
      expect(c['saving']()).toBeFalse(); expect(c['ready']()).toBeTrue();
      expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    });
  }
  it('disables saving after a record disappears', () => {
    service.update.and.returnValue(throwError(() => ({ status: 404 })));
    const c = start('4'); c['submit'](); expect(c['ready']()).toBeFalse();
    expect(c['error']()).toContain('n’existe plus');
  });
  for (const status of [401, 403]) {
    it(`leaves ${status} navigation to the global interceptor`, () => {
      service.create.and.returnValue(throwError(() => ({ status })));
      const c = start(); fill(c); c['submit'](); expect(c['error']()).toBe('');
      expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    });
  }
});
