import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { LeaveService } from './leave.service';
import { LeavesPageComponent } from './leaves-page.component';
import { LeaveResponse } from './leave.models';
describe('LeavesPageComponent', () => {
  let fixture: ComponentFixture<LeavesPageComponent>; let service: jasmine.SpyObj<LeaveService>;
  let role: CrmRole; let dialog: jasmine.SpyObj<MatDialog>;
  const pending: LeaveResponse = { id: 1, employeeId: '101', startDate: '2099-01-01', endDate: '2099-01-02', reason: 'Repos', status: 'PENDING', validatorUsername: null, createdAt: '', updatedAt: '' };
  beforeEach(async () => {
    role = CrmRole.EMPLOYEE;
    service = jasmine.createSpyObj('LeaveService', ['list', 'delete', 'approve', 'reject', 'options', 'associations', 'associate']);
    service.list.and.returnValue(of([pending])); service.delete.and.returnValue(of(undefined));
    service.approve.and.returnValue(of({ ...pending, status: 'APPROVED', validatorUsername: 'admin' }));
    service.reject.and.returnValue(of({ ...pending, status: 'REJECTED', validatorUsername: 'admin' }));
    service.options.and.returnValue(of({ users: [{ id: 2, label: 'employee' }], employees: [{ id: 101, label: 'EMP-01' }] }));
    service.associations.and.returnValue(of([])); service.associate.and.returnValue(of({ username: 'employee', userId: 2, employeeId: '101' }));
    dialog = jasmine.createSpyObj('MatDialog', ['open']); dialog.open.and.returnValue({ afterClosed: () => of(true) } as any);
    await TestBed.configureTestingModule({ imports: [LeavesPageComponent], providers: [provideZonelessChangeDetection(), provideRouter([]),
      { provide: LeaveService, useValue: service }, { provide: MatDialog, useValue: dialog },
      { provide: AuthService, useValue: { hasAnyRole: (roles: CrmRole[]) => roles.includes(role) } }]
    }).compileComponents(); fixture = TestBed.createComponent(LeavesPageComponent);
  });
  function component(): any { return fixture.componentInstance; }
  it('shows loading, then the list', () => {
    const stream = new Subject<LeaveResponse[]>(); service.list.and.returnValue(stream); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chargement des demandes');
    stream.next([pending]); stream.complete(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('101'); expect(fixture.nativeElement.textContent).toContain('Repos');
  });
  it('shows empty', () => { service.list.and.returnValue(of([])); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Aucune demande'); });
  it('shows a safe error and supports retry', () => {
    service.list.and.returnValue(throwError(() => ({ status: 500, message: 'private diagnostics' }))); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Impossible de charger'); expect(fixture.nativeElement.textContent).not.toContain('private diagnostics');
    service.list.and.returnValue(of([pending])); component().load(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('101');
  });
  it('explains an unassociated employee access failure', () => { service.list.and.returnValue(throwError(() => ({ status: 403 }))); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('association Employee'); });
  it('shows personal actions without administrative actions', () => {
    fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('Nouvelle demande'); expect(text).toContain('Modifier'); expect(text).toContain('Supprimer'); expect(text).not.toContain('Approuver'); expect(text).not.toContain('Refuser');
    component().act(pending, 'approve'); expect(service.approve).not.toHaveBeenCalled();
  });
  for (const admin of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it(`shows decisions for ${admin}, without personal edit/create`, () => {
      role = admin; fixture.detectChanges(); const text = fixture.nativeElement.textContent;
      expect(text).toContain('Approuver'); expect(text).toContain('Refuser'); expect(text).not.toContain('Nouvelle demande'); expect(text).not.toContain('Modifier');
    });
  }
  for (const action of ['approve', 'reject'] as const) {
    it(`confirms ${action} and refreshes`, () => {
      role = CrmRole.ADMIN; fixture.detectChanges(); component().act(pending, action);
      expect(dialog.open).toHaveBeenCalled(); expect(service[action]).toHaveBeenCalledWith(1); expect(service.list).toHaveBeenCalledTimes(2);
    });
  }
  it('does not decide a completed request', () => { role = CrmRole.ADMIN; fixture.detectChanges(); component().act({ ...pending, status: 'APPROVED' }, 'reject'); expect(dialog.open).not.toHaveBeenCalled(); });
  it('hides all mutations for decided requests', () => { service.list.and.returnValue(of([{ ...pending, status: 'APPROVED' }])); fixture.detectChanges(); expect(fixture.nativeElement.textContent).not.toContain('Modifier'); expect(fixture.nativeElement.textContent).not.toContain('Supprimer'); });
  it('requires confirmation before deleting', () => {
    fixture.detectChanges(); dialog.open.and.returnValue({ afterClosed: () => of(false) } as any); component().act(pending, 'delete'); expect(service.delete).not.toHaveBeenCalled();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as any); component().act(pending, 'delete'); expect(service.delete).toHaveBeenCalledWith(1);
  });
  it('blocks repeated decisions and reports conflict', () => {
    role = CrmRole.ADMIN; fixture.detectChanges(); const stream = new Subject<LeaveResponse>(); service.approve.and.returnValue(stream);
    component().act(pending, 'approve'); component().act(pending, 'approve'); expect(service.approve).toHaveBeenCalledTimes(1);
    stream.error({ status: 409 }); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('déjà été décidée'); expect(component().busy()).toBeFalse();
  });
  it('loads administrative associations and saves selected IDs', () => {
    role = CrmRole.ADMIN; fixture.detectChanges(); component().loadDirectory(); fixture.detectChanges();
    expect(service.options).toHaveBeenCalled(); component().linkForm.setValue({ userId: 2, employeeId: 101 }); component().associate();
    expect(service.associate).toHaveBeenCalledWith(2, 101);
  });
  it('does not expose association loading to employees', () => { fixture.detectChanges(); component().loadDirectory(); expect(service.options).not.toHaveBeenCalled(); });
});
