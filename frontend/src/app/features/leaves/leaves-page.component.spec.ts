import { provideZonelessChangeDetection, signal } from '@angular/core';
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
    service = jasmine.createSpyObj('LeaveService', ['list', 'delete', 'approve', 'reject', 'options', 'associations', 'associate', 'reassign']);
    service.list.and.returnValue(of([pending])); service.delete.and.returnValue(of(undefined));
    service.approve.and.returnValue(of({ ...pending, status: 'APPROVED', validatorUsername: 'admin' }));
    service.reject.and.returnValue(of({ ...pending, status: 'REJECTED', validatorUsername: 'admin' }));
    service.options.and.returnValue(of({ users: [{ id: 2, label: 'employee' }], employees: [{ id: 101, label: 'EMP-01' }] }));
    service.associations.and.returnValue(of([])); service.associate.and.returnValue(of({ username: 'employee', userId: 2, employeeId: '101' }));
    service.reassign.and.returnValue(of({ username: 'employee', userId: 2, employeeId: '101' }));
    dialog = jasmine.createSpyObj('MatDialog', ['open']); dialog.open.and.returnValue({ afterClosed: () => of(true) } as any);
    await TestBed.configureTestingModule({ imports: [LeavesPageComponent], providers: [provideZonelessChangeDetection(), provideRouter([]),
      { provide: LeaveService, useValue: service }, { provide: MatDialog, useValue: dialog },
      { provide: AuthService, useValue: { sessionGeneration:0,getCurrentUser:()=>null,hasAnyRole: (roles: CrmRole[]) => roles.includes(role) } }]
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
  it('routes administrators to the shared association interface without legacy writes', () => {
    role = CrmRole.ADMIN; fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/users#associations"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Réaffecter');
    expect(service.associate).not.toHaveBeenCalled(); expect(service.reassign).not.toHaveBeenCalled();
  });
  it('does not expose association administration to employees', () => {
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('a[href="/users#associations"]')).toBeNull();
    expect(service.options).not.toHaveBeenCalled();
  });
  it('filters the entire authorized history by status and keyword before pagination',()=>{
    service.list.and.returnValue(of(Array.from({length:24},(_,i)=>({...pending,id:i+1,reason:'Reason '+i,status:i%2?'APPROVED':'PENDING'} as LeaveResponse))));
    fixture.detectChanges();expect(component().visible().length).toBe(10);
    component().paginate({pageIndex:2,pageSize:10});expect(component().visible().length).toBe(4);
    component().filter('Reason 23','APPROVED');expect(component().pageIndex()).toBe(0);expect(component().visible().map((r:LeaveResponse)=>r.id)).toEqual([24]);
    expect(service.list).toHaveBeenCalledTimes(1);
  });
  it('cancels an old-session confirmation before any write and clears the history',()=>{
    const generation=signal(0),auth=TestBed.inject(AuthService) as any;
    Object.defineProperty(auth,'sessionGeneration',{get:()=>generation()});auth.getCurrentUser=()=>({username:'same',generation:generation()});
    const confirmation=new Subject<boolean>();dialog.open.and.returnValue({afterClosed:()=>confirmation} as any);
    fixture.detectChanges();component().selected.set(pending);component().act(pending,'delete');
    service.list.and.returnValue(of([]));generation.set(1);fixture.detectChanges();confirmation.next(true);confirmation.complete();
    expect(service.delete).not.toHaveBeenCalled();expect(component().leaves()).toEqual([]);expect(component().selected()).toBeNull();
  });
  it('never fetches a history for CLIENT',()=>{role=CrmRole.CLIENT;fixture.detectChanges();expect(service.list).not.toHaveBeenCalled();});
  it('allows the existing administrative delete only after confirmation',()=>{
    role=CrmRole.ADMIN;fixture.detectChanges();component().act(pending,'delete');
    expect(dialog.open).toHaveBeenCalled();expect(service.delete).toHaveBeenCalledOnceWith(1);
  });
});
