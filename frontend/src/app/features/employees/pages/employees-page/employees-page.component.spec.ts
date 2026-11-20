import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { EmployeePage, EmployeeResponse } from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';
import { EmployeesPageComponent } from './employees-page.component';
import { MAT_DIALOG_DEFAULT_OPTIONS, MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { AuthService } from '../../../../core/services/auth.service';
import { CrmRole } from '../../../../core/models/role.model';

describe('EmployeesPageComponent', () => {
  it('links to employee creation and editing', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/employees/new"]').textContent).toContain('Nouvel employé');
    expect(fixture.nativeElement.querySelector('a[aria-label="Modifier EMP001"]').getAttribute('href')).toBe('/employees/1/edit');
  });
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
  let auth: jasmine.SpyObj<AuthService>;
  let snackBar: MatSnackBar;

  beforeEach(async () => {
    employeeService = jasmine.createSpyObj<EmployeeService>('EmployeeService', ['getEmployeesPage', 'deleteEmployee']);
    employeeService.getEmployeesPage.and.returnValue(of(page));
    employeeService.deleteEmployee.and.returnValue(of(undefined));
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    auth.hasAnyRole.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [EmployeesPageComponent],
      providers: [
        { provide: EmployeeService, useValue: employeeService },
        { provide: AuthService, useValue: auth },
        { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { enterAnimationDuration: 0, exitAnimationDuration: 0 } },
        provideRouter([]),
        provideZonelessChangeDetection()
      ]
    }).compileComponents();

    snackBar = TestBed.inject(MatSnackBar);
    spyOn(snackBar, 'open').and.callThrough();
    fixture = TestBed.createComponent(EmployeesPageComponent);
    component = fixture.componentInstance;
  });

  async function confirmDeletion(confirm = true): Promise<void> {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Supprimer EMP001"]').click();
    const loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const dialog = await loader.getHarness(MatDialogHarness);
    expect(await dialog.getContentText()).toContain('EMP001 — John Doe');
    expect(await dialog.getContentText()).toContain('irréversible');
    const button = await dialog.getHarness(MatButtonHarness.with({ text: confirm ? 'Supprimer' : 'Annuler' }));
    await button.click();
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  async function notificationText(): Promise<string> {
    const loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    return (await loader.getHarness(MatSnackBarHarness)).getMessage();
  }

  it('cancels the real confirmation dialog without DELETE or reload', async () => {
    fixture.detectChanges();
    await confirmDeletion(false);
    expect(employeeService.deleteEmployee).not.toHaveBeenCalled();
    expect(employeeService.getEmployeesPage).toHaveBeenCalledTimes(1);
    expect(component['confirming']()).toBeFalse();
  });

  it('confirms exactly one DELETE, notifies and preserves search, sorting and page size', async () => {
    fixture.detectChanges();
    component['searchControl'].setValue('John');
    component['sortBy'].set('lastName');
    component['direction'].set('desc');
    component['pageSize'].set(25);
    employeeService.getEmployeesPage.calls.reset();
    await confirmDeletion();
    expect(employeeService.deleteEmployee).toHaveBeenCalledOnceWith(1);
    expect(snackBar.open).toHaveBeenCalledWith(jasmine.stringMatching('EMP001.*supprimé'), 'Fermer', jasmine.any(Object));
    expect(employeeService.getEmployeesPage).toHaveBeenCalledOnceWith({ page: 0, size: 25, sortBy: 'lastName', direction: 'desc', keyword: 'John' });
    expect(component['deletingId']()).toBeNull();
    expect(await notificationText()).toContain('EMP001 a été supprimé');
  });

  it('prevents duplicate dialogs and duplicate DELETE while pending', async () => {
    const pending = new Subject<void>();
    employeeService.deleteEmployee.and.returnValue(pending);
    fixture.detectChanges();
    component['deleteEmployee'](employee);
    component['deleteEmployee'](employee);
    expect(TestBed.inject(MatDialog).openDialogs.length).toBe(1);
    const loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const dialog = await loader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Supprimer' }))).click();
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    component['deleteEmployee'](employee);
    fixture.detectChanges();
    expect(employeeService.deleteEmployee).toHaveBeenCalledOnceWith(1);
    expect(fixture.nativeElement.querySelector('button[aria-label="Supprimer EMP001"]').disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Suppression en cours');
    pending.next(); pending.complete();
    expect(component['deletingId']()).toBeNull();
  });

  it('retains the employee and releases deleting state on failure', async () => {
    employeeService.deleteEmployee.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    await confirmDeletion();
    expect(component['employees']()).toEqual([employee]);
    expect(employeeService.getEmployeesPage).toHaveBeenCalledTimes(1);
    expect(component['deletingId']()).toBeNull();
    expect(component['confirming']()).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith(jasmine.stringMatching('Impossible de supprimer'), 'Fermer', jasmine.any(Object));
    expect(fixture.nativeElement.textContent).toContain('EMP001');
  });

  it('returns to the previous page when its last employee is deleted', async () => {
    fixture.detectChanges();
    component['pageIndex'].set(2);
    await confirmDeletion();
    expect(employeeService.getEmployeesPage.calls.mostRecent().args[0].page).toBe(1);
  });

  it('keeps the current page when other employees remain', async () => {
    fixture.detectChanges();
    component['employees'].set([employee, { ...employee, id: 2, employeeCode: 'EMP002' }]);
    component['pageIndex'].set(2);
    await confirmDeletion();
    expect(employeeService.getEmployeesPage.calls.mostRecent().args[0].page).toBe(2);
  });

  it('preserves page 2 requested during deletion of the last employee on page 3', async () => {
    fixture.detectChanges();
    component['onPageChange']({ pageIndex: 2, pageSize: 10, length: 21 });
    const deletion = new Subject<void>();
    const navigation = new Subject<EmployeePage>();
    const reload = new Subject<EmployeePage>();
    employeeService.deleteEmployee.and.returnValue(deletion);
    await confirmDeletion();
    employeeService.getEmployeesPage.calls.reset();
    employeeService.getEmployeesPage.and.returnValues(navigation, reload);
    component['onPageChange']({ pageIndex: 1, pageSize: 10, length: 21 });
    expect(component['employees']()).toEqual([employee]);
    deletion.next(); deletion.complete();
    expect(component['pageIndex']()).toBe(1);
    expect(employeeService.getEmployeesPage.calls.allArgs().map(([params]) => params.page)).toEqual([1, 1]);
    expect(navigation.observed).toBeFalse();
    const remaining = { ...employee, id: 2, employeeCode: 'EMP002' };
    reload.next({ ...page, content: [remaining], number: 1, totalElements: 20 }); reload.complete();
    navigation.next(page); navigation.complete();
    expect(component['employees']()).toEqual([remaining]);
    expect(component['pageIndex']()).toBe(1);
    expect(component['totalElements']()).toBe(20);
  });

  for (const change of ['search', 'sort', 'pageSize', 'unsubmitted search']) {
    it('does not reuse the old last-row decision after changing ' + change, async () => {
      fixture.detectChanges();
      component['onPageChange']({ pageIndex: 2, pageSize: 10, length: 21 });
      const deletion = new Subject<void>();
      const pendingList = new Subject<EmployeePage>();
      employeeService.deleteEmployee.and.returnValue(deletion);
      await confirmDeletion();
      employeeService.getEmployeesPage.and.returnValue(pendingList);
      if (change === 'search' || change === 'unsubmitted search') {
        component['searchControl'].setValue('Jane');
        if (change === 'search') component['searchEmployees']();
      } else if (change === 'sort') {
        component['onSortChange']({ active: 'lastName', direction: 'desc' });
      } else {
        component['onPageChange']({ pageIndex: 1, pageSize: 25, length: 51 });
      }
      const requestedPage = component['pageIndex']();
      const requestedSize = component['pageSize']();
      deletion.next(); deletion.complete();
      expect(component['pageIndex']()).toBe(requestedPage);
      expect(employeeService.getEmployeesPage.calls.mostRecent().args[0]).toEqual({
        page: requestedPage, size: requestedSize,
        sortBy: change === 'sort' ? 'lastName' : 'id',
        direction: change === 'sort' ? 'desc' : 'asc',
        ...(change === 'search' || change === 'unsubmitted search' ? { keyword: 'Jane' } : {})
      });
      pendingList.next({ ...page, content: [], totalElements: 0 }); pendingList.complete();
    });
  }

  for (const role of Object.values(CrmRole)) {
    it('restricts delete visibility and invocation for ' + role, () => {
      auth.hasAnyRole.and.callFake((roles) => roles.includes(role));
      fixture.detectChanges();
      const allowed = role === CrmRole.ADMIN || role === CrmRole.SUPER_ADMIN;
      expect(!!fixture.nativeElement.querySelector('button[aria-label="Supprimer EMP001"]')).toBe(allowed);
      if (!allowed) {
        component['deleteEmployee'](employee);
        expect(TestBed.inject(MatDialog).openDialogs.length).toBe(0);
        expect(employeeService.deleteEmployee).not.toHaveBeenCalled();
      }
    });
  }

  it('uses a local message for the documented not-found response', async () => {
    employeeService.deleteEmployee.and.returnValue(throwError(() => ({ status: 404, error: { message: 'internal details' } })));
    fixture.detectChanges();
    await confirmDeletion();
    expect(await notificationText()).toBe('Cet employé est introuvable ou a déjà été supprimé.');
  });

  for (const status of [400, 409, 500]) {
    for (const message of [
      'Cet employé est associé à des données existantes.',
      'Upstream http://employee-service:8081 unavailable',
      'Backend node crm-db-01:3306 unreachable',
      'SQLException: internal table users',
      '<b>Internal error</b>',
      { details: 'arbitrary payload' }
    ]) {
      it('renders only a local fallback for ' + status + ': ' + JSON.stringify(message), async () => {
        employeeService.deleteEmployee.and.returnValue(throwError(() => ({ status, error: { message } })));
        fixture.detectChanges();
        await confirmDeletion();
        expect(await notificationText()).toBe('Impossible de supprimer cet employé. Réessayez plus tard.');
      });
    }
  }

  it('keeps the post-delete response when an older list arrives later', async () => {
    fixture.detectChanges();
    const deletion = new Subject<void>();
    const oldList = new Subject<EmployeePage>();
    const recentList = new Subject<EmployeePage>();
    employeeService.deleteEmployee.and.returnValue(deletion);
    await confirmDeletion();
    employeeService.getEmployeesPage.and.returnValues(oldList, recentList);
    component['searchEmployees']();
    deletion.next(); deletion.complete();
    expect(oldList.observed).toBeFalse();
    expect(component['loading']()).toBeTrue();
    recentList.next({ ...page, content: [], totalElements: 0 }); recentList.complete();
    oldList.next(page); oldList.complete();
    fixture.detectChanges();
    expect(component['employees']()).toEqual([]);
    expect(component['totalElements']()).toBe(0);
    expect(component['errorMessage']()).toBe('');
    expect(component['loading']()).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('John');
  });

  it('ignores old errors and keeps loading until the latest request finishes', () => {
    const oldList = new Subject<EmployeePage>();
    const recentList = new Subject<EmployeePage>();
    employeeService.getEmployeesPage.and.returnValues(oldList, recentList);
    fixture.detectChanges();
    component['onPageChange']({ pageIndex: 2, pageSize: 25, length: 100 });
    oldList.error(new Error('old failure'));
    expect(component['loading']()).toBeTrue();
    expect(component['errorMessage']()).toBe('');
    recentList.next({ ...page, totalElements: 51 }); recentList.complete();
    expect(component['employees']()).toEqual([employee]);
    expect(component['totalElements']()).toBe(51);
    expect(component['pageIndex']()).toBe(2);
    expect(component['pageSize']()).toBe(25);
    expect(component['loading']()).toBeFalse();
  });

  it('cancels list loading when the component is destroyed', () => {
    const pending = new Subject<EmployeePage>();
    employeeService.getEmployeesPage.and.returnValue(pending);
    fixture.detectChanges();
    expect(pending.observed).toBeTrue();
    fixture.destroy();
    expect(pending.observed).toBeFalse();
    expect(component['loading']()).toBeFalse();
  });

  it('can reload after the latest list request fails', () => {
    employeeService.getEmployeesPage.and.returnValues(throwError(() => new Error('failed')), of(page));
    fixture.detectChanges();
    expect(component['errorMessage']()).not.toBe('');
    component['searchEmployees']();
    expect(component['errorMessage']()).toBe('');
    expect(component['employees']()).toEqual([employee]);
  });

  for (const status of [401, 403]) {
    it('leaves authentication handling to the interceptor for ' + status, async () => {
      employeeService.deleteEmployee.and.returnValue(throwError(() => ({ status })));
      fixture.detectChanges();
      await confirmDeletion();
      expect(snackBar.open).not.toHaveBeenCalled();
      expect(component['employees']()).toEqual([employee]);
      expect(component['deletingId']()).toBeNull();
    });
  }

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

  it('provides a link to the employee detail page', () => {
    fixture.detectChanges();

    const detailLink = fixture.nativeElement.querySelector('a[aria-label="Voir le détail de EMP001"]');

    expect(detailLink.getAttribute('href')).toBe('/employees/1');
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
