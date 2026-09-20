import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { EmployeeWithDepartmentResponse } from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeDetailComponent } from './employee-detail.component';

describe('EmployeeDetailComponent', () => {
  const response: EmployeeWithDepartmentResponse = {
    employee: {
      id: 12,
      employeeCode: 'EMP012',
      firstName: 'Marie',
      lastName: 'Dupont',
      email: 'marie.dupont@example.com',
      phone: '0102030405',
      jobTitle: 'Responsable RH',
      hireDate: '2023-05-15',
      salary: 52000,
      address: '12 rue de Paris',
      dateOfBirth: '1990-02-10',
      gender: 'Féminin',
      status: 'ACTIF'
    },
    department: {
      id: 2,
      name: 'Ressources humaines',
      code: 'RH',
      description: 'Gestion du personnel',
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    }
  };
  let fixture: ComponentFixture<EmployeeDetailComponent>;
  let component: EmployeeDetailComponent;
  let employeeService: jasmine.SpyObj<EmployeeService>;
  let router: jasmine.SpyObj<Router>;

  async function createComponent(id = '12'): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [EmployeeDetailComponent],
      providers: [
        { provide: EmployeeService, useValue: employeeService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id }) } } },
        { provide: Router, useValue: router },
        provideZonelessChangeDetection()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeDetailComponent);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    employeeService = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['getEmployeeDetails']);
    employeeService.getEmployeeDetails.and.returnValue(of(response));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);
  });

  it('creates the component and loads the employee id from the route', async () => {
    await createComponent();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(employeeService.getEmployeeDetails).toHaveBeenCalledWith(12);
  });

  it('displays employee and department information', async () => {
    await createComponent();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Marie');
    expect(fixture.nativeElement.textContent).toContain('Responsable RH');
    expect(fixture.nativeElement.textContent).toContain('Ressources humaines');
  });

  it('displays the absence message when the department is null', async () => {
    employeeService.getEmployeeDetails.and.returnValue(of({ ...response, department: null }));
    await createComponent();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun département associé');
  });

  it('shows the loading state while the request is pending', async () => {
    const pendingRequest = new Subject<EmployeeWithDepartmentResponse>();
    employeeService.getEmployeeDetails.and.returnValue(pendingRequest.asObservable());
    await createComponent();
    fixture.detectChanges();

    expect(component['loading']()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain("Chargement de l'employé...");

    pendingRequest.next(response);
    pendingRequest.complete();
  });

  it('shows a not found state for a missing employee', async () => {
    employeeService.getEmployeeDetails.and.returnValue(throwError(() => ({ status: 404 })));
    await createComponent();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Employé introuvable');
  });

  it('shows an error message when loading fails', async () => {
    employeeService.getEmployeeDetails.and.returnValue(throwError(() => ({ status: 500 })));
    await createComponent();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Une erreur est survenue lors du chargement de l'employé.");
  });

  it('shows a not found state without calling the API for an invalid id', async () => {
    await createComponent('invalid');
    fixture.detectChanges();

    expect(employeeService.getEmployeeDetails).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Employé introuvable');
  });

  it('navigates back to the employee list', async () => {
    await createComponent();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('button').click();

    expect(router.navigate).toHaveBeenCalledWith(['/employees']);
  });
});
