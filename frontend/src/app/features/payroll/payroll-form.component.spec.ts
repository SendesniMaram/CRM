import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CustomerService } from '../customers/customer.service';
import { EmployeePage } from '../employees/models/employee.models';
import { EmployeeService } from '../employees/services/employee.service';
import { FeeReferencePickerComponent } from '../fees/fee-reference-picker.component';
import { PayrollFormComponent } from './payroll-form.component';
import { PayrollRequest, PayrollResponse } from './payroll.models';
import { PayrollService } from './payroll.service';

describe('PayrollFormComponent', () => {
  const request: PayrollRequest = { employeeId: 'EMP-legacy', baseSalary: 2000, bonuses: null,
    deductions: null, taxAmount: null, payPeriod: '2026-10', paymentDate: null, currency: null, status: 'À payer' };
  const row: PayrollResponse = { id: 4, ...request, netSalary: 123.45, published: false, createdAt: null, updatedAt: null };
  let fixture: ComponentFixture<PayrollFormComponent>;
  let service: jasmine.SpyObj<PayrollService>;
  let auth: jasmine.SpyObj<AuthService>;
  let customers: jasmine.SpyObj<CustomerService>;
  let employees: jasmine.SpyObj<EmployeeService>;
  let params: Subject<ReturnType<typeof convertToParamMap>>;

  beforeEach(async () => {
    params = new Subject(); service = jasmine.createSpyObj('PayrollService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(row)); service.create.and.returnValue(of(row)); service.update.and.returnValue(of(row));
    auth = jasmine.createSpyObj('AuthService', ['hasAnyRole']); auth.hasAnyRole.and.returnValue(true);
    customers = jasmine.createSpyObj('CustomerService', ['list']);
    employees = jasmine.createSpyObj('EmployeeService', ['getEmployeesPage']);
    await TestBed.configureTestingModule({ imports: [PayrollFormComponent], providers: [provideRouter([]),
      provideZonelessChangeDetection(), { provide: PayrollService, useValue: service },
      { provide: AuthService, useValue: auth }, { provide: CustomerService, useValue: customers },
      { provide: EmployeeService, useValue: employees }, { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
    ] }).compileComponents();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(PayrollFormComponent); fixture.detectChanges();
  });
  function start(id?: string) {
    params.next(convertToParamMap(id === undefined ? {} : { id })); fixture.detectChanges();
    return fixture.componentInstance;
  }
  function fill(c: PayrollFormComponent) {
    c['form'].patchValue({ employeeId: ' EMP-legacy ', baseSalary: 2000, payPeriod: ' 2026-10 ', status: ' À payer ' });
  }
  it('requires nonblank employee, period and status and a positive finite base salary', () => {
    const c = start(); c['submit'](); expect(service.create).not.toHaveBeenCalled(); fill(c);
    expect(c['form'].valid).toBeTrue();
    for (const key of ['employeeId', 'payPeriod', 'status'] as const) {
      c['form'].controls[key].setValue(' '); expect(c['form'].controls[key].invalid).toBeTrue();
    }
    for (const value of [null, 0, -1, Infinity, NaN]) {
      c['form'].controls.baseSalary.setValue(value); expect(c['form'].controls.baseSalary.invalid).toBeTrue();
    }
  });
  it('allows nullable optional amounts and signed finite adjustments as the API does', () => {
    const c = start(); fill(c);
    for (const key of ['bonuses', 'deductions', 'taxAmount'] as const) {
      for (const value of [null, 0, -2.5, 100]) {
        c['form'].controls[key].setValue(value); expect(c['form'].controls[key].valid).toBeTrue();
      }
      for (const value of [Infinity, NaN]) {
        c['form'].controls[key].setValue(value); expect(c['form'].controls[key].invalid).toBeTrue();
      }
    }
  });
  it('enforces DTO lengths and real calendar dates without a period, currency or status enum', () => {
    const c = start(); fill(c);
    c['form'].patchValue({ payPeriod: 'Période libre', currency: 'LIBRE', status: 'Statut libre', paymentDate: '2028-02-29' });
    expect(c['form'].valid).toBeTrue();
    for (const value of ['2027-02-29', '2026-04-31', '14/10/2026', 'invalid']) {
      c['form'].controls.paymentDate.setValue(value); expect(c['form'].controls.paymentDate.invalid).toBeTrue();
    }
    c['form'].controls.paymentDate.setValue(''); expect(c['form'].controls.paymentDate.valid).toBeTrue();
    for (const [key, max] of [['employeeId', 50], ['payPeriod', 50], ['currency', 10], ['status', 50]] as const) {
      c['form'].controls[key].setValue('x'.repeat(max)); expect(c['form'].controls[key].valid).toBeTrue();
      c['form'].controls[key].setValue('x'.repeat(max + 1)); expect(c['form'].controls[key].hasError('maxlength')).toBeTrue();
    }
  });
  it('creates the exact trimmed DTO without netSalary, id or timestamps', () => {
    const c = start(); fill(c); c['form'].controls.currency.setValue(' '); c['submit']();
    expect(service.create).toHaveBeenCalledOnceWith(request);
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/payroll']);
    expect(TestBed.inject(MatSnackBar).open).toHaveBeenCalledWith('Fiche de paie créée.', 'Fermer', { duration: 5000 });
    expect(employees.getEmployeesPage).not.toHaveBeenCalled(); expect(customers.list).not.toHaveBeenCalled();
  });
  it('prefills and updates existing values while preserving arbitrary legacy employee references', () => {
    const c = start('4'); expect(c['form'].controls.employeeId.value).toBe('EMP-legacy');
    c['form'].controls.bonuses.setValue(150); c['submit']();
    expect(service.update).toHaveBeenCalledOnceWith(4, { ...request, bonuses: 150 });
    expect(service.create).not.toHaveBeenCalled();
    expect(TestBed.inject(MatSnackBar).open).toHaveBeenCalledWith('Fiche de paie modifiée.', 'Fermer', { duration: 5000 });
  });
  it('selects an employee through the existing paged directory and sends its numeric ID as a string', async () => {
    const directory = { content: [{ id: 7, employeeCode: 'EMP-7', firstName: 'Employé', lastName: 'Test', salary: 999 }],
      totalElements: 1, totalPages: 1, number: 0, size: 25 } as EmployeePage;
    employees.getEmployeesPage.and.returnValue(of(directory));
    const c = start(); fill(c); c['lookup'].set(true); fixture.detectChanges();
    const picker = fixture.debugElement.query(By.directive(FeeReferencePickerComponent)).componentInstance as FeeReferencePickerComponent;
    picker['search'].setValue(' EMP '); picker['find'](); fixture.detectChanges();
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open(); await select.clickOptions({ text: 'Employé Test — 7' });
    expect(employees.getEmployeesPage).toHaveBeenCalledWith({ page: 0, size: 25, sortBy: 'employeeCode', direction: 'asc', keyword: 'EMP' });
    expect(c['form'].controls.employeeId.value).toBe('7');
    // Employee salary must never overwrite the payroll base salary.
    expect(c['form'].controls.baseSalary.value).toBe(2000);
    c['submit'](); expect(service.create).toHaveBeenCalledOnceWith({ ...request, employeeId: '7' });
    expect(customers.list).not.toHaveBeenCalled();
  });
  it('shows directory failures without losing the existing reference or blocking a valid form', () => {
    employees.getEmployeesPage.and.returnValue(throwError(() => ({ status: 503 })));
    const c = start(); fill(c); c['lookup'].set(true); fixture.detectChanges();
    const picker = fixture.debugElement.query(By.directive(FeeReferencePickerComponent)).componentInstance as FeeReferencePickerComponent;
    picker['find'](); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Annuaire indisponible');
    c['submit'](); expect(service.create).toHaveBeenCalledOnceWith(request);
  });
  it('prevents duplicate submissions and selection changes during save', () => {
    const pending = new Subject<PayrollResponse>(); service.create.and.returnValue(pending);
    const c = start(); fill(c); c['submit'](); c['submit'](); c['selectEmployee']('8');
    expect(service.create).toHaveBeenCalledTimes(1); expect(c['form'].controls.employeeId.value).toBe(' EMP-legacy ');
    pending.next(row); pending.complete(); expect(c['saving']()).toBeFalse();
  });
  it('denies writes and employee selection after administrative permission is lost', () => {
    const c = start(); fill(c); auth.hasAnyRole.and.returnValue(false); c['submit'](); c['selectEmployee']('8');
    expect(service.create).not.toHaveBeenCalled(); expect(c['form'].controls.employeeId.value).toBe(' EMP-legacy ');
  });
  it('does not write for invalid routes, missing records or reads still loading', () => {
    const c = start('invalid'); fill(c); c['submit'](); expect(service.get).not.toHaveBeenCalled();
    service.get.and.returnValue(throwError(() => ({ status: 404 })));
    params.next(convertToParamMap({ id: '4' })); c['submit'](); expect(c['error']()).toContain('introuvable');
    service.get.and.returnValue(new Subject<PayrollResponse>()); params.next(convertToParamMap({ id: '5' }));
    fill(c); c['submit'](); expect(service.update).not.toHaveBeenCalled();
  });
  it('cancels obsolete record reads when the route changes', () => {
    const old = new Subject<PayrollResponse>(); service.get.and.returnValue(old);
    const c = start('4'); params.next(convertToParamMap({})); old.next(row); fill(c); c['submit']();
    expect(service.update).not.toHaveBeenCalled(); expect(service.create).toHaveBeenCalledOnceWith(request);
  });
  it('maps server validation without exposing raw messages and allows retry', () => {
    service.create.and.returnValues(throwError(() => ({ status: 400, error: { message: 'baseSalary: PRIVATE SQL' } })), of(row));
    const c = start(); fill(c); c['submit'](); fixture.detectChanges();
    expect(c['form'].controls.baseSalary.hasError('server')).toBeTrue();
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
