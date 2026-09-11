import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { of, Subject, throwError } from 'rxjs';
import { EmployeePage, EmployeeResponse } from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';
import { EmployeesPageComponent } from './employees-page.component';

describe('EmployeesPageComponent', () => {
  const employee: EmployeeResponse = {
    id: 1,
    employeeCode: 'EMP001',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    jobTitle: 'Software Engineer',
    hireDate: '2024-01-15',
    salary: 60000,
    status: 'ACTIVE'
  };
  const page: EmployeePage = {
    content: [employee],
    totalElements: 1,
    totalPages: 1,
    size: 10,
    number: 0,
    numberOfElements: 1,
    first: true,
    last: true,
    empty: false,
    sort: { empty: false, sorted: true, unsorted: false },
    pageable: {
      offset: 0,
      pageNumber: 0,
      pageSize: 10,
      paged: true,
      unpaged: false,
      sort: { empty: false, sorted: true, unsorted: false }
    }
  };
  let fixture: ComponentFixture<EmployeesPageComponent>;
  let component: EmployeesPageComponent;
  let employeeService: jasmine.SpyObj<EmployeeService>;

  beforeEach(async () => {
    employeeService = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['getEmployeesPage']);
    employeeService.getEmployeesPage.and.returnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [EmployeesPageComponent],
      providers: [
        { provide: EmployeeService, useValue: employeeService },
        provideZonelessChangeDetection()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeesPageComponent);
    component = fixture.componentInstance;
  });

  it('creates the component', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('loads employees with the default page parameters', () => {
    fixture.detectChanges();

    expect(employeeService.getEmployeesPage).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sortBy: 'id',
      direction: 'asc'
    });
  });

  it('displays loaded employee data', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('EMP001');
    expect(fixture.nativeElement.textContent).toContain('John');
    expect(fixture.nativeElement.textContent).toContain('Doe');
    expect(fixture.nativeElement.textContent).toContain('Software Engineer');
    expect(fixture.nativeElement.textContent).toContain('ACTIVE');
  });

  it('shows the loading state while the request is pending', () => {
    const pendingRequest = new Subject<EmployeePage>();
    employeeService.getEmployeesPage.and.returnValue(pendingRequest.asObservable());

    fixture.detectChanges();

    expect(component['loading']()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Chargement des employés...');

    pendingRequest.next(page);
    pendingRequest.complete();
    fixture.detectChanges();

    expect(component['loading']()).toBeFalse();
  });

  it('shows the empty state when no employees are returned', () => {
    employeeService.getEmployeesPage.and.returnValue(of({ ...page, content: [], totalElements: 0 }));

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun employé trouvé.');
  });

  it('shows the generic error state when loading fails', () => {
    employeeService.getEmployeesPage.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible de charger les employés.');
  });

  it('searches from the first page with the keyword', () => {
    fixture.detectChanges();
    employeeService.getEmployeesPage.calls.reset();
    component['searchControl'].setValue('John');

    component['searchEmployees']();

    expect(employeeService.getEmployeesPage).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sortBy: 'id',
      direction: 'asc',
      keyword: 'John'
    });
  });

  it('loads the selected page from the paginator', () => {
    fixture.detectChanges();
    employeeService.getEmployeesPage.calls.reset();

    component['onPageChange']({ pageIndex: 2, pageSize: 25, length: 100 } as PageEvent);

    expect(employeeService.getEmployeesPage).toHaveBeenCalledWith({
      page: 2,
      size: 25,
      sortBy: 'id',
      direction: 'asc'
    });
  });

  it('loads the selected backend sort from MatSort', () => {
    fixture.detectChanges();
    employeeService.getEmployeesPage.calls.reset();

    component['onSortChange']({ active: 'lastName', direction: 'desc' } as Sort);

    expect(employeeService.getEmployeesPage).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sortBy: 'lastName',
      direction: 'desc'
    });
  });
});
