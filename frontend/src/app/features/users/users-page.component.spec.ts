import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { UserDirectoryService } from './user-directory.service';
import { UsersPageComponent } from './users-page.component';

describe('UsersPageComponent', () => {
  let service: jasmine.SpyObj<UserDirectoryService>; let role: CrmRole;
  const body = { firstName: 'Test', lastName: 'Employee', username: 'employee-created', email: 'test@example.test', password: 'test-only-password', phone: '' };
  beforeEach(() => {
    role = CrmRole.ADMIN;
    service = jasmine.createSpyObj('UserDirectoryService', ['list', 'createEmployee']);
    service.list.and.returnValue(of([])); service.createEmployee.and.returnValue(of(undefined));
    TestBed.configureTestingModule({ imports: [UsersPageComponent], providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter([]),
      { provide: UserDirectoryService, useValue: service }, { provide: AuthService, useValue: { getCurrentUser: () => null, sessionGeneration: 0, hasAnyRole: (roles: CrmRole[]) => roles.includes(role) } }] });
  });
  function setup() { const fixture = TestBed.createComponent(UsersPageComponent); fixture.detectChanges(); return { fixture, page: fixture.componentInstance as any }; }
  for (const current of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it('never loads or creates for ' + current, () => {
      role = current; const { fixture, page } = setup(); page.form.setValue(body); page.create();
      expect(service.list).not.toHaveBeenCalled(); expect(service.createEmployee).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('form')).toBeNull();
    });
  }
  for (const current of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it('confirms creation only after success for ' + current, async () => {
      role = current; const { fixture, page } = setup(); page.form.setValue(body); page.create();
      await fixture.whenStable(); fixture.detectChanges();
      expect(service.createEmployee).toHaveBeenCalledWith(body); expect(page.form.controls.password.value).toBe('');
      expect(fixture.nativeElement.textContent).toContain('Étape compte terminée');
      expect(fixture.nativeElement.querySelector('a[href="/users#associations"]')).not.toBeNull();
      page.create(); expect(service.createEmployee).toHaveBeenCalledTimes(1);
    });
  }
  it('blocks invalid fields and double submissions', () => {
    const { page } = setup(); page.create(); expect(service.createEmployee).not.toHaveBeenCalled();
    page.form.setValue({ ...body, email: 'invalid' }); page.create(); expect(service.createEmployee).not.toHaveBeenCalled();
    page.form.setValue(body); const pending = new Subject<void>(); service.createEmployee.and.returnValue(pending);
    page.create(); page.create(); expect(service.createEmployee).toHaveBeenCalledTimes(1); expect(page.createdUsername()).toBe('');
    pending.error({ status: 503 }); expect(page.busy()).toBeFalse(); expect(page.form.controls.password.value).toBe('');
    expect(page.form.controls.username.value).toBe(body.username);
  });
  for (const status of [400, 401, 403, 409, 502, 503]) {
    it('does not falsely confirm on ' + status + ' and preserves non-secret fields', () => {
      service.createEmployee.and.returnValue(throwError(() => ({ status, error: { message: 'private diagnostics' } })));
      const { fixture, page } = setup(); page.form.setValue(body); page.create(); fixture.detectChanges();
      expect(page.createdUsername()).toBe(''); expect(page.error()).not.toContain('private diagnostics');
      expect(page.error()).not.toBe(''); expect(page.form.controls.password.value).toBe('');
      expect(page.form.controls.email.value).toBe(body.email); expect(fixture.nativeElement.textContent).not.toContain('Étape compte terminée');
    });
  }
  it('retains successful creation when independent list reload fails', async () => {
    const { fixture, page } = setup(); service.list.and.returnValue(throwError(() => ({ status: 503 })));
    page.form.setValue(body); page.create(); await fixture.whenStable();
    expect(page.createdUsername()).toBe(body.username); expect(page.listError()).not.toBe('');
    page.create(); expect(service.createEmployee).toHaveBeenCalledTimes(1);
    service.list.and.returnValue(of([])); page.load(); expect(page.createdUsername()).toBe(body.username);
  });
  it('shows identity, all roles and account flags without a free role selector', () => {
    service.list.and.returnValue(of([{ id: '42', username: 'multi-role', firstName: 'Test', lastName: 'Account', email: 'test@example.test',
      phone: null, enabled: false, accountNonLocked: false, roleType: CrmRole.ADMIN, roles: [CrmRole.ADMIN, CrmRole.EMPLOYEE], createdAt: null }]));
    const { fixture } = setup(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('multi-role'); expect(text).toContain('ADMIN, EMPLOYEE'); expect(text).toContain('Désactivé'); expect(text).toContain('Verrouillé');
    expect(fixture.nativeElement.querySelector('form select, form mat-select')).toBeNull();
  });
  function accounts() {
    return Array.from({length:26},(_,i)=>({id:i===0?'9223372036854775807':String(i+1),username:'account-'+i,
      firstName:'Person',lastName:String(i),email:'account-'+i+'@example.test',phone:null,
      roles:[i%2?CrmRole.CLIENT:CrmRole.EMPLOYEE],roleType:CrmRole.EMPLOYEE,
      enabled:i!==0,accountNonLocked:i!==2,createdAt:null}));
  }
  it('paginates the full directory, preserves long IDs and filters across all pages', () => {
    service.list.and.returnValue(of(accounts())); const {page}=setup();
    expect(page.visibleUsers().length).toBe(10);
    expect(page.visibleUsers()[0].id).toBe('9223372036854775807');
    page.paginate({pageIndex:2,pageSize:10}); expect(page.visibleUsers().length).toBe(6);
    page.filter('account-25','',''); expect(page.pageIndex()).toBe(0);
    expect(page.visibleUsers().map((user:any)=>user.username)).toEqual(['account-25']);
    expect(service.list).toHaveBeenCalledTimes(1);
  });
  it('combines role and account-state filters without offering role mutation', () => {
    service.list.and.returnValue(of(accounts())); const {page,fixture}=setup();
    page.filter('',CrmRole.EMPLOYEE,'locked'); expect(page.filteredUsers().map((u:any)=>u.username)).toEqual(['account-2']);
    page.filter('',CrmRole.EMPLOYEE,'disabled'); expect(page.filteredUsers()[0].id).toBe('9223372036854775807');
    page.filter('no-match','','');fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Aucun compte ne correspond');
    expect(service.createEmployee).not.toHaveBeenCalled();
  });
  it('clears detail and filters on session change and ignores a late directory response', () => {
    const session=signal(0); const auth=TestBed.inject(AuthService) as any;
    Object.defineProperty(auth,'sessionGeneration',{get:()=>session()});auth.getCurrentUser=()=>({username:'same-name',session:session()});
    const old=new Subject<any>();service.list.and.returnValue(old);const {page,fixture}=setup();
    page.query.set('old');page.selectedUser.set(accounts()[0]);
    service.list.and.returnValue(of([]));session.set(1);fixture.detectChanges();old.next(accounts());
    expect(page.users()).toEqual([]);expect(page.selectedUser()).toBeNull();expect(page.query()).toBe('');
  });
});
