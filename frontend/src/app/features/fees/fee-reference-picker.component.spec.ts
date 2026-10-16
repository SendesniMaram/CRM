import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Subject, of, throwError } from 'rxjs';
import { CustomerPage } from '../customers/customer.models';
import { CustomerService } from '../customers/customer.service';
import { EmployeePage } from '../employees/models/employee.models';
import { EmployeeService } from '../employees/services/employee.service';
import { FeeReferencePickerComponent } from './fee-reference-picker.component';

describe('FeeReferencePickerComponent', () => {
  const customerPage: CustomerPage = { content: [{ id: 2, name: 'Client test', email: null, phone: null,
    company: null, address: null, createdAt: null, updatedAt: null }], totalElements: 30, totalPages: 2, number: 0, size: 25 };
  const employeePage = { ...customerPage, content: [{ id: 7, employeeCode: 'EMP-7', firstName: 'Employé', lastName: 'Test', salary: 0 }] } as unknown as EmployeePage;
  let fixture: ComponentFixture<FeeReferencePickerComponent>;
  let customers: jasmine.SpyObj<CustomerService>;
  let employees: jasmine.SpyObj<EmployeeService>;
  beforeEach(async () => {
    customers = jasmine.createSpyObj('CustomerService', ['list']); customers.list.and.returnValue(of(customerPage));
    employees = jasmine.createSpyObj('EmployeeService', ['getEmployeesPage']); employees.getEmployeesPage.and.returnValue(of(employeePage));
    await TestBed.configureTestingModule({ imports: [FeeReferencePickerComponent], providers: [provideZonelessChangeDetection(),
      { provide: CustomerService, useValue: customers }, { provide: EmployeeService, useValue: employees }] }).compileComponents();
    fixture = TestBed.createComponent(FeeReferencePickerComponent);
  });
  function start(kind: 'employee' | 'customer') {
    fixture.componentRef.setInput('kind', kind); fixture.detectChanges(); return fixture.componentInstance;
  }
  it('loads employees only on demand with paged server search and converts the selected ID to string', async () => {
    const c = start('employee'); expect(employees.getEmployeesPage).not.toHaveBeenCalled();
    c['search'].setValue(' EMP '); c['find'](); fixture.detectChanges();
    expect(employees.getEmployeesPage).toHaveBeenCalledWith({ page: 0, size: 25, sortBy: 'employeeCode', direction: 'asc', keyword: 'EMP' });
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open();
    expect(await Promise.all((await select.getOptions()).map(option => option.getText()))).toEqual(['Employé Test — 7']);
    spyOn(c.referenceSelected, 'emit'); await select.clickOptions({ text: 'Employé Test — 7' });
    expect(c.referenceSelected.emit).toHaveBeenCalledOnceWith('7');
    expect(customers.list).not.toHaveBeenCalled();
  });
  it('uses the existing CustomerService and supports paged search', () => {
    const c = start('customer'); c['load'](); c['paginate'](1);
    expect(customers.list.calls.mostRecent().args[0].page).toBe(1);
    c['search'].setValue(' Client '); c['find']();
    expect(customers.list.calls.mostRecent().args[0]).toEqual({ page: 0, size: 25, sortBy: 'name', direction: 'asc', keyword: 'Client' });
    spyOn(c.referenceSelected, 'emit'); c['select'](2); expect(c.referenceSelected.emit).toHaveBeenCalledOnceWith('2');
  });
  it('handles empty and unavailable directories and can retry', () => {
    customers.list.and.returnValues(throwError(() => ({ status: 503 })), of({ ...customerPage, content: [], totalPages: 0, totalElements: 0 }));
    const c = start('customer'); c['load'](); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Annuaire indisponible'); expect(c['loading']()).toBeFalse();
    c['load'](); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Aucun résultat');
  });
  it('ignores obsolete responses and shows loading until the latest request finishes', () => {
    const old = new Subject<CustomerPage>(); const latest = new Subject<CustomerPage>();
    customers.list.and.returnValues(old, latest);
    const c = start('customer'); c['find'](); c['find'](); old.next(customerPage);
    expect(c['loading']()).toBeTrue(); expect(c['choices']()).toEqual([]);
    latest.next(customerPage); latest.complete(); expect(c['choices']().length).toBe(1); expect(c['loading']()).toBeFalse();
  });
  it('never converts unsafe IDs or emits an unknown reference', () => {
    customers.list.and.returnValue(of({ ...customerPage, content: [{ ...customerPage.content[0], id: Number.MAX_SAFE_INTEGER + 1 }] }));
    const c = start('customer'); c['load'](); spyOn(c.referenceSelected, 'emit');
    c['select'](Number.MAX_SAFE_INTEGER + 1); c['select'](999); expect(c.referenceSelected.emit).not.toHaveBeenCalled();
    expect(c['choices']()).toEqual([]);
  });
  it('blocks requests and selections during save', () => {
    const c = start('employee'); c['load'](); fixture.componentRef.setInput('disabled', true);
    spyOn(c.referenceSelected, 'emit'); c['select'](7); c['find'](); c['paginate'](1);
    expect(c.referenceSelected.emit).not.toHaveBeenCalled(); expect(employees.getEmployeesPage).toHaveBeenCalledTimes(1);
  });
  it('unsubscribes pending directory reads on destruction', () => {
    const pending = new Subject<CustomerPage>(); customers.list.and.returnValue(pending);
    const c = start('customer'); c['load'](); fixture.destroy();
    expect(pending.observed).toBeFalse();
  });
});
