import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { EmployeeResponse } from '../../models/employee.models';
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
  let fixture: ComponentFixture<EmployeeFormComponent>;
  let router: Router;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  async function create(id?: string) {
    params = new BehaviorSubject(convertToParamMap(id === undefined ? {} : { id }));
    await TestBed.configureTestingModule({
      imports: [EmployeeFormComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]),
        { provide: EmployeeService, useValue: service },
        { provide: ActivatedRoute, useValue: { paramMap: params, snapshot: { paramMap: params.value } } }]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(EmployeeFormComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['getEmployeeById', 'createEmployee', 'updateEmployee']);
    service.getEmployeeById.and.returnValue(of(employee));
    service.createEmployee.and.returnValue(of(employee));
    service.updateEmployee.and.returnValue(of(employee));
  });

  it('starts creation with an empty form and required code and salary', async () => {
    const component = await create();
    expect(fixture.nativeElement.textContent).toContain('Nouvel employé');
    expect(component['form'].controls.employeeCode.value).toBe('');
    expect(component['form'].controls.salary.value).toBeNull();
    expect(component['form'].controls.employeeCode.hasError('required')).toBeTrue();
    expect(component['form'].controls.salary.hasError('required')).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    component['submit']();
    expect(service.createEmployee).not.toHaveBeenCalled();
    expect(service.getEmployeeById).not.toHaveBeenCalled();
  });

  it('loads and prefills every editable field including departmentId', async () => {
    const component = await create('12');
    expect(service.getEmployeeById).toHaveBeenCalledWith(12);
    expect(fixture.nativeElement.textContent).toContain('Modifier l’employé');
    const { id, ...fields } = employee;
    expect(component['form'].getRawValue()).toEqual(fields);
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
    component['form'].controls.address.setValue('x'.repeat(501));
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
    expect(service.updateEmployee).toHaveBeenCalledWith(12, jasmine.objectContaining({ firstName: 'Anne', departmentId: 2 }));
    expect(service.createEmployee).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/employees', 12]);
  });

  it('allows clearing an optional department on update', async () => {
    const component = await create('12');
    component['form'].controls.departmentId.setValue(null);
    component['submit']();
    expect(service.updateEmployee.calls.mostRecent().args[1].departmentId).toBeUndefined();
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
});
