import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControlName } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { DepartmentResponse, EmployeeResponse } from '../../models/employee.models';
import { DepartmentService } from '../../../departments/services/department.service';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeFormComponent } from './employee-form.component';

describe('EmployeeFormComponent', () => {
  const employee = {
    id: 12, employeeCode: 'EMP012', firstName: 'Marie', lastName: 'Dupont',
    email: 'marie@example.test', phone: '12345678', jobTitle: 'Responsable',
    hireDate: '2020-01-01', salary: 52000, address: 'Rue du Port',
    dateOfBirth: '1990-01-01', gender: 'F', status: 'ACTIF', departmentId: 2
  } satisfies EmployeeResponse;
  let service: jasmine.SpyObj<EmployeeService>;
  let departmentService: jasmine.SpyObj<DepartmentService>;
  const departments: DepartmentResponse[] = [
    { id: 2, name: 'Engineering', code: 'ENG', description: '', createdAt: '', updatedAt: '' },
    { id: 5, name: 'Finance', code: 'FIN', description: '', createdAt: '', updatedAt: '' }
  ];
  let fixture: ComponentFixture<EmployeeFormComponent>;
  let router: Router;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  function inputFor(name: string): HTMLInputElement {
    return fixture.debugElement.queryAll(By.directive(FormControlName))
      .find((element) => element.injector.get(FormControlName).name === name)!.nativeElement;
  }

  async function create(id?: string) {
    params = new BehaviorSubject(convertToParamMap(id === undefined ? {} : { id }));
    await TestBed.configureTestingModule({
      imports: [EmployeeFormComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]),
        { provide: EmployeeService, useValue: service },
        { provide: DepartmentService, useValue: departmentService },
        { provide: ActivatedRoute, useValue: { paramMap: params, snapshot: { paramMap: params.value } } }]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(EmployeeFormComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('accepts 255 characters and empty address but blocks 256 characters before HTTP', async () => {
    const component = await create();
    component['form'].patchValue({ employeeCode: 'BOUNDARY', salary: 1000 });
    const control = component['form'].controls.address;
    control.setValue('é'.repeat(255));
    expect(control.valid).toBeTrue();
    expect(component['form'].valid).toBeTrue();
    control.setValue('é'.repeat(256));
    expect(control.hasError('maxlength')).toBeTrue();
    component['submit']();
    expect(service.createEmployee).not.toHaveBeenCalled();
    control.setValue('');
    expect(control.valid).toBeTrue();
  });

  beforeEach(() => {
    departmentService = jasmine.createSpyObj<DepartmentService>('DepartmentService', ['getDepartments']);
    departmentService.getDepartments.and.returnValue(of(departments));
    service = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['getEmployeeById', 'createEmployee', 'updateEmployee']);
    service.getEmployeeById.and.returnValue(of(employee));
    service.createEmployee.and.returnValue(of(employee));
    service.updateEmployee.and.returnValue(of(employee));
  });

  it('starts creation with an empty form and required code and salary', async () => {
    const component = await create();
    expect(fixture.nativeElement.textContent).toContain('Nouvel employé');
    expect(component['form'].controls.employeeCode.value).toBe('');
    expect(inputFor('employeeCode').readOnly).toBeFalse();
    expect(component['form'].controls.salary.value).toBeNull();
    expect(component['form'].controls.employeeCode.hasError('required')).toBeTrue();
    expect(component['form'].controls.salary.hasError('required')).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    component['submit']();
    expect(service.createEmployee).not.toHaveBeenCalled();
    expect(service.getEmployeeById).not.toHaveBeenCalled();
  });

  it('loads the form with a visible read-only code and editable other fields', async () => {
    const component = await create('12');
    expect(service.getEmployeeById).toHaveBeenCalledWith(12);
    expect(fixture.nativeElement.textContent).toContain('Modifier l’employé');
    const { id, ...fields } = employee;
    expect(component['form'].getRawValue()).toEqual(fields);
    const codeInput = inputFor('employeeCode');
    expect(codeInput.value).toBe('EMP012');
    expect(codeInput.readOnly).toBeTrue();
    expect(codeInput.disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Le code employé ne peut pas être modifié après la création.');
    expect(inputFor('firstName').readOnly).toBeFalse();
  });

  it('rejects whitespace code, negative salary and accepts zero', async () => {
    const component = await create();
    component['form'].patchValue({ employeeCode: '   ', salary: -1 });
    expect(component['form'].controls.employeeCode.hasError('required')).toBeTrue();
    expect(component['form'].controls.salary.hasError('min')).toBeTrue();
    component['form'].patchValue({ employeeCode: 'EMP', salary: 0 });
    expect(component['form'].valid).toBeTrue();
  });

  it('validates field lengths, email and optional positive integer department', async () => {
    const component = await create();
    for (const field of component['textFields']) {
      const control = component['form'].controls[field.name];
      control.setValue('x'.repeat(field.max + 1));
      expect(control.hasError('maxlength')).withContext(field.name).toBeTrue();
      control.setValue('');
    }
    component['form'].controls.address.setValue('x'.repeat(256));
    expect(component['form'].controls.address.hasError('maxlength')).toBeTrue();
    component['form'].controls.email.setValue('invalid');
    expect(component['form'].controls.email.hasError('email')).toBeTrue();
    component['form'].controls.email.setValue('   ');
    expect(component['form'].controls.email.valid).toBeTrue();
    for (const value of [0, -1, 1.5]) {
      component['form'].controls.departmentId.setValue(value);
      expect(component['form'].controls.departmentId.invalid).toBeTrue();
    }
    for (const value of [null, 2]) {
      component['form'].controls.departmentId.setValue(value);
      expect(component['form'].controls.departmentId.valid).toBeTrue();
    }
  });

  it('rejects future hire dates, accepts today and adds no birth date restriction', async () => {
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 8, 27, 12));
      const component = await create();
      component['form'].controls.hireDate.setValue('2026-09-28');
      expect(component['form'].controls.hireDate.hasError('futureDate')).toBeTrue();
      component['form'].controls.hireDate.setValue('2026-09-27');
      expect(component['form'].controls.hireDate.valid).toBeTrue();
      component['form'].controls.dateOfBirth.setValue('2099-01-01');
      expect(component['form'].controls.dateOfBirth.valid).toBeTrue();
    } finally { jasmine.clock().uninstall(); }
  });

  it('creates a trimmed payload with omitted empty fields and navigates using returned id', async () => {
    const component = await create();
    component['form'].patchValue({ employeeCode: ' EMP012 ', salary: 0, departmentId: 2, firstName: ' Marie ', lastName: '  ' });
    component['submit']();
    const payload = service.createEmployee.calls.mostRecent().args[0];
    expect(payload.employeeCode).toBe('EMP012');
    expect(payload.firstName).toBe('Marie');
    expect(payload.lastName).toBeUndefined();
    expect(payload.salary).toBe(0);
    expect(payload.departmentId).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith(['/employees', 12]);
  });

  it('updates the loaded employee, preserves department and navigates to detail', async () => {
    const component = await create('12');
    component['form'].controls.firstName.setValue('Anne');
    component['submit']();
    expect(service.updateEmployee).toHaveBeenCalledWith(12, jasmine.objectContaining({ employeeCode: 'EMP012', firstName: 'Anne', departmentId: 2 }));
    expect(service.createEmployee).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/employees', 12]);
  });

  it('allows clearing an optional department on update', async () => {
    const component = await create('12');
    component['form'].controls.departmentId.setValue(null);
    component['submit']();
    expect(service.updateEmployee.calls.mostRecent().args[1].departmentId).toBeUndefined();
  });

  it('preserves the exact existing code in the update payload', async () => {
    service.getEmployeeById.and.returnValue(of({ ...employee, employeeCode: ' EMP012 ' }));
    const component = await create('12');
    component['submit']();
    expect(service.updateEmployee.calls.mostRecent().args[1].employeeCode).toBe(' EMP012 ');
  });

  it('makes the code editable again when navigating from edit to creation', async () => {
    await create('12');
    params.next(convertToParamMap({}));
    fixture.detectChanges();
    const codeInput = inputFor('employeeCode');
    expect(codeInput.readOnly).toBeFalse();
    expect(codeInput.value).toBe('');
  });

  it('blocks duplicate submissions and displays pending state', async () => {
    const pending = new Subject<EmployeeResponse>();
    service.createEmployee.and.returnValue(pending);
    const component = await create();
    component['form'].patchValue({ employeeCode: 'EMP', salary: 1 });
    component['submit']();
    component['submit']();
    fixture.detectChanges();
    expect(service.createEmployee).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement en cours');
    pending.error({ status: 500 });
    expect(component['saving']()).toBeFalse();
  });

  it('shows accessible API errors without navigation and preserves entered data', async () => {
    service.createEmployee.and.returnValue(throwError(() => ({ status: 500 })));
    const component = await create();
    component['form'].patchValue({ employeeCode: 'EMP', salary: 1 });
    component['submit']();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible d’enregistrer');
    expect(component['form'].controls.employeeCode.value).toBe('EMP');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('handles update validation errors', async () => {
    service.updateEmployee.and.returnValue(throwError(() => ({ status: 400 })));
    const component = await create('12');
    component['submit']();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Enregistrement refusé');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  for (const id of ['invalid', '0', '-1', '1.5']) {
    it(`rejects invalid edit id ${id} without any API request`, async () => {
      const component = await create(id);
      expect(service.getEmployeeById).not.toHaveBeenCalled();
      expect(fixture.nativeElement.textContent).toContain('Employé introuvable');
      component['submit']();
      expect(service.createEmployee).not.toHaveBeenCalled();
      expect(service.updateEmployee).not.toHaveBeenCalled();
    });
  }

  it('shows loading and prevents submission before edit data arrives', async () => {
    const pending = new Subject<EmployeeResponse>();
    service.getEmployeeById.and.returnValue(pending);
    const component = await create('12');
    expect(fixture.nativeElement.textContent).toContain('Chargement de l’employé');
    component['submit']();
    expect(service.updateEmployee).not.toHaveBeenCalled();
    pending.next(employee);
    pending.complete();
    expect(component['loading']()).toBeFalse();
  });

  it('handles a 404 while loading', async () => {
    service.getEmployeeById.and.returnValue(throwError(() => ({ status: 404 })));
    await create('12');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Employé introuvable');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('handles a 404 while saving', async () => {
    service.updateEmployee.and.returnValue(throwError(() => ({ status: 404 })));
    const component = await create('12');
    component['submit']();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Employé introuvable');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('handles generic load failures without exposing an editable form', async () => {
    service.getEmployeeById.and.returnValue(throwError(() => ({ status: 500 })));
    await create('12');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible de charger');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('reloads when route id changes and cancels pending loads on destruction', async () => {
    await create('12');
    const pending = new Subject<EmployeeResponse>();
    service.getEmployeeById.and.returnValue(pending);
    params.next(convertToParamMap({ id: '13' }));
    expect(service.getEmployeeById).toHaveBeenCalledWith(13);
    expect(pending.observed).toBeTrue();
    fixture.destroy();
    expect(pending.observed).toBeFalse();
  });

  function select() {
    return TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  }

  it('loads named options in creation without selecting a department and sends the selected numeric ID', async () => {
    const component = await create();
    const selector = await select();
    expect(departmentService.getDepartments).toHaveBeenCalledTimes(1);
    expect(component['form'].controls.departmentId.value).toBeNull();
    expect(await selector.getValueText()).toBe('Aucun département');
    await selector.open();
    expect(await Promise.all((await selector.getOptions()).map((option) => option.getText())))
      .toEqual(['Aucun département', 'Engineering', 'Finance']);
    await selector.clickOptions({ text: 'Finance' });
    expect(component['form'].controls.departmentId.value).toBe(5);
    component['form'].patchValue({ employeeCode: 'EMP', salary: 1 });
    component['submit']();
    expect(service.createEmployee).toHaveBeenCalledWith(jasmine.objectContaining({ departmentId: 5 }));
  });

  it('preselects the existing department and allows explicit clearing through the select', async () => {
    const component = await create('12');
    const selector = await select();
    expect(await selector.getValueText()).toBe('Engineering');
    await selector.open();
    await selector.clickOptions({ text: 'Aucun département' });
    expect(component['form'].controls.departmentId.value).toBeNull();
    component['submit']();
    expect(service.updateEmployee.calls.mostRecent().args[1].departmentId).toBeUndefined();
  });

  for (const employeeFirst of [true, false]) {
    it(`preselects correctly when ${employeeFirst ? 'Employee' : 'Departments'} arrives first`, async () => {
      const employeePending = new Subject<EmployeeResponse>();
      const departmentPending = new Subject<DepartmentResponse[]>();
      service.getEmployeeById.and.returnValue(employeePending);
      departmentService.getDepartments.and.returnValue(departmentPending);
      const component = await create('12');
      if (employeeFirst) {
        employeePending.next(employee);
        employeePending.complete();
        const selector = await select();
        expect(await selector.isDisabled()).toBeTrue();
        expect(fixture.nativeElement.textContent).toContain('Chargement des départements');
        expect(component['form'].getRawValue().departmentId).toBe(2);
        departmentPending.next(departments);
        departmentPending.complete();
      } else {
        departmentPending.next(departments);
        departmentPending.complete();
        employeePending.next(employee);
        employeePending.complete();
      }
      const selector = await select();
      expect(await selector.getValueText()).toBe('Engineering');
      expect(await selector.isDisabled()).toBeFalse();
      expect(component['form'].getRawValue().departmentId).toBe(2);
    });
  }

  it('represents a missing department and preserves its ID when another field changes', async () => {
    service.getEmployeeById.and.returnValue(of({ ...employee, departmentId: 7 }));
    const component = await create('12');
    const selector = await select();
    expect(await selector.getValueText()).toBe('Département indisponible — ID 7');
    component['form'].controls.firstName.setValue('Anne');
    component['submit']();
    expect(service.updateEmployee).toHaveBeenCalledWith(12, jasmine.objectContaining({ firstName: 'Anne', departmentId: 7 }));
  });

  it('preserves the form on Department failure, hides backend details and retries only Departments', async () => {
    const secret = 'SQL stack trace http://internal-server/private';
    departmentService.getDepartments.and.returnValue(throwError(() => ({ status: 500, error: { message: secret } })));
    const component = await create('12');
    const selector = await select();
    component['form'].controls.firstName.setValue('Anne');
    expect(await selector.isDisabled()).toBeTrue();
    expect(await selector.getValueText()).toBe('Département indisponible — ID 2');
    expect(fixture.nativeElement.textContent).toContain('Impossible de charger les départements');
    expect(fixture.nativeElement.textContent).not.toContain(secret);
    const save = new Subject<EmployeeResponse>();
    service.updateEmployee.and.returnValue(save);
    component['submit']();
    expect(service.updateEmployee).toHaveBeenCalledWith(12, jasmine.objectContaining({ firstName: 'Anne', departmentId: 2 }));
    save.error({ status: 500 });
    expect(await selector.isDisabled()).toBeTrue();
    const pending = new Subject<DepartmentResponse[]>();
    departmentService.getDepartments.and.returnValue(pending);
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find((button) => button.textContent?.includes('Réessayer'))!;
    retry.click();
    fixture.detectChanges();
    component['loadDepartments']();
    expect(departmentService.getDepartments).toHaveBeenCalledTimes(2);
    expect(await selector.isDisabled()).toBeTrue();
    pending.next(departments);
    pending.complete();
    expect(await selector.getValueText()).toBe('Engineering');
    expect(await selector.isDisabled()).toBeFalse();
    expect(component['form'].controls.firstName.value).toBe('Anne');
    expect(service.getEmployeeById).toHaveBeenCalledTimes(1);
  });

  it('allows creation without a department when the list is empty', async () => {
    departmentService.getDepartments.and.returnValue(of([]));
    const component = await create();
    const selector = await select();
    expect(await selector.isDisabled()).toBeFalse();
    expect(await selector.getValueText()).toBe('Aucun département');
    expect(fixture.nativeElement.textContent).toContain('Aucun département disponible');
    component['form'].patchValue({ employeeCode: 'EMP', salary: 1 });
    expect(component['form'].valid).toBeTrue();
    component['submit']();
    expect(service.createEmployee.calls.mostRecent().args[0].departmentId).toBeUndefined();
  });

  it('disables the select during saving, preserves the ID and restores it after an Employee error', async () => {
    const pending = new Subject<EmployeeResponse>();
    service.updateEmployee.and.returnValue(pending);
    const component = await create('12');
    const selector = await select();
    component['submit']();
    expect(await selector.isDisabled()).toBeTrue();
    expect(service.updateEmployee.calls.mostRecent().args[1].departmentId).toBe(2);
    pending.error({ status: 400 });
    expect(await selector.isDisabled()).toBeFalse();
    expect(await selector.getValueText()).toBe('Engineering');
  });

  it('keeps the select disabled if Departments finish loading during a save', async () => {
    const departmentsPending = new Subject<DepartmentResponse[]>();
    const savePending = new Subject<EmployeeResponse>();
    departmentService.getDepartments.and.returnValue(departmentsPending);
    service.updateEmployee.and.returnValue(savePending);
    const component = await create('12');
    component['submit']();
    departmentsPending.next(departments);
    departmentsPending.complete();
    const selector = await select();
    expect(await selector.isDisabled()).toBeTrue();
    expect(service.updateEmployee.calls.mostRecent().args[1].departmentId).toBe(2);
    savePending.next(employee);
    savePending.complete();
    expect(await selector.isDisabled()).toBeFalse();
  });

  it('cancels a pending Department subscription when the form is destroyed', async () => {
    const pending = new Subject<DepartmentResponse[]>();
    departmentService.getDepartments.and.returnValue(pending);
    await create();
    expect(pending.observed).toBeTrue();
    fixture.destroy();
    expect(pending.observed).toBeFalse();
  });
});
