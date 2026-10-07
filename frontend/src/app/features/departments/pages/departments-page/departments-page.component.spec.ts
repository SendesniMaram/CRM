import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MAT_DIALOG_DEFAULT_OPTIONS, MatDialog } from '@angular/material/dialog';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { DepartmentResponse } from '../../../employees/models/employee.models';
import { DepartmentService } from '../../services/department.service';
import { DepartmentsPageComponent } from './departments-page.component';

describe('DepartmentsPageComponent', () => {
  const department: DepartmentResponse = { id: 1, name: 'Informatique', code: 'IT', description: 'Support', createdAt: '', updatedAt: '' };
  let service: jasmine.SpyObj<DepartmentService>;
  let auth: jasmine.SpyObj<AuthService>;
  let fixture: ComponentFixture<DepartmentsPageComponent>;
  let snackBar: MatSnackBar;

  beforeEach(async () => {
    service = jasmine.createSpyObj<DepartmentService>('DepartmentService', ['getDepartments', 'deleteDepartment']);
    service.getDepartments.and.returnValue(of([department]));
    service.deleteDepartment.and.returnValue(of(undefined));
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    auth.hasAnyRole.and.returnValue(true);
    await TestBed.configureTestingModule({
      imports: [DepartmentsPageComponent],
      providers: [provideRouter([]), provideZonelessChangeDetection(),
        { provide: DepartmentService, useValue: service }, { provide: AuthService, useValue: auth },
        { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { enterAnimationDuration: 0, exitAnimationDuration: 0 } }]
    }).compileComponents();
    snackBar = TestBed.inject(MatSnackBar);
    spyOn(snackBar, 'open').and.callThrough();
    fixture = TestBed.createComponent(DepartmentsPageComponent);
  });

  async function confirm(yes: boolean): Promise<void> {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Supprimer Informatique"]').click();
    const dialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatDialogHarness);
    expect(await dialog.getContentText()).toContain('Informatique');
    expect(await dialog.getContentText()).toContain('irréversible');
    await (await dialog.getHarness(MatButtonHarness.with({ text: yes ? 'Supprimer' : 'Annuler' }))).click();
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  it('shows loading while GET is pending and then renders the response', () => {
    const pending = new Subject<DepartmentResponse[]>();
    service.getDepartments.and.returnValue(pending);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chargement des départements');
    pending.next([department]); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Informatique');
    expect(fixture.nativeElement.textContent).toContain('IT');
    expect(fixture.nativeElement.textContent).toContain('Support');
    expect(fixture.nativeElement.querySelector('a[href="/departments/new"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/departments/1/edit"]')).not.toBeNull();
  });

  it('shows an empty state', () => {
    service.getDepartments.and.returnValue(of([])); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun département disponible');
  });

  it('shows a safe load error and retries', () => {
    service.getDepartments.and.returnValue(throwError(() => ({ status: 500, error: { message: 'private backend details' } })));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible de charger');
    expect(fixture.nativeElement.textContent).not.toContain('private backend details');
    service.getDepartments.and.returnValue(of([department]));
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(service.getDepartments).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Informatique');
  });

  it('does not DELETE when confirmation is cancelled', async () => {
    await confirm(false); expect(service.deleteDepartment).not.toHaveBeenCalled();
    expect(service.getDepartments).toHaveBeenCalledTimes(1);
  });

  it('deletes only after confirmation and reloads the list', async () => {
    await confirm(true);
    expect(service.deleteDepartment).toHaveBeenCalledOnceWith(1);
    expect(service.getDepartments).toHaveBeenCalledTimes(2);
    expect(snackBar.open).toHaveBeenCalledWith('Département supprimé.', 'Fermer', jasmine.any(Object));
  });

  it('keeps the row on backend refusal and shows an understandable message', async () => {
    service.deleteDepartment.and.returnValue(throwError(() => ({ status: 409, error: { message: 'private backend details' } })));
    await confirm(true);
    expect(snackBar.open).toHaveBeenCalledWith(jasmine.stringMatching('Suppression refusée'), 'Fermer', jasmine.any(Object));
    expect(fixture.nativeElement.textContent).toContain('Informatique');
    expect(service.getDepartments).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance['deletingId']()).toBeNull();
  });

  it('handles a generic deletion failure without exposing backend diagnostics', async () => {
    service.deleteDepartment.and.returnValue(throwError(() => ({ status: 500 })));
    await confirm(true);
    expect(snackBar.open).toHaveBeenCalledWith(jasmine.stringMatching('Impossible de supprimer'), 'Fermer', jasmine.any(Object));
  });

  it('blocks repeat deletion while a request is pending', async () => {
    const pending = new Subject<void>(); service.deleteDepartment.and.returnValue(pending);
    await confirm(true);
    fixture.componentInstance['deleteDepartment'](department);
    expect(service.deleteDepartment).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[aria-label="Supprimer Informatique"]').disabled).toBeTrue();
    pending.next(); pending.complete();
  });

  it('hides management actions and blocks direct deletion without an admin role', () => {
    auth.hasAnyRole.and.returnValue(false); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/departments/new"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('button[aria-label="Supprimer Informatique"]')).toBeNull();
    fixture.componentInstance['deleteDepartment'](department);
    expect(service.deleteDepartment).not.toHaveBeenCalled();
  });

  it('rechecks roles after confirmation', async () => {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Supprimer Informatique"]').click();
    auth.hasAnyRole.and.returnValue(false);
    const dialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Supprimer' }))).click();
    expect(service.deleteDepartment).not.toHaveBeenCalled();
  });

  it('unsubscribes a pending list request on destruction', () => {
    const pending = new Subject<DepartmentResponse[]>(); service.getDepartments.and.returnValue(pending);
    fixture.detectChanges(); expect(pending.observed).toBeTrue();
    fixture.destroy(); expect(pending.observed).toBeFalse();
  });

  afterEach(() => { TestBed.inject(MatDialog).closeAll(); snackBar.dismiss(); });
});
