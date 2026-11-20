import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError, map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { UserDirectoryService } from './user-directory.service';
import { UsersPageComponent } from './users-page.component';

describe('UsersPageComponent', () => {
  let service: jasmine.SpyObj<UserDirectoryService>; let role: CrmRole;
  const body = { firstName: 'Test', lastName: 'Employee', username: 'employee-created', email: 'test@example.test', password: 'test-only-password', phone: '' };
  beforeEach(() => {
    role = CrmRole.ADMIN;
    service = jasmine.createSpyObj('UserDirectoryService', ['list', 'search', 'createEmployee', 'create', 'update', 'setRoles', 'setEnabled']);
    service.list.and.returnValue(of([])); service.search.and.callFake(()=>service.list().pipe(map(rows=>({content:rows,number:0,totalPages:rows.length?1:0,totalElements:rows.length})))); service.createEmployee.and.returnValue(of(undefined));
    service.create.and.returnValue(of({} as any)); service.update.and.returnValue(of({} as any));
    service.setRoles.and.returnValue(of({} as any)); service.setEnabled.and.returnValue(of({} as any));
    TestBed.configureTestingModule({ imports: [UsersPageComponent], providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter([]),
      { provide: UserDirectoryService, useValue: service }, { provide: AuthService, useValue: { getCurrentUser: () => null, sessionGeneration: 0, hasAnyRole: (roles: CrmRole[]) => roles.includes(role) } }] });
  });
  function setup() { const fixture = TestBed.createComponent(UsersPageComponent); fixture.detectChanges(); return { fixture, page: fixture.componentInstance as any }; }
  it('expires the current session immediately after changing its own roles', () => {
    role = CrmRole.SUPER_ADMIN;
    const auth = TestBed.inject(AuthService) as any;
    auth.getCurrentUser = () => ({ username: 'self' }); auth.getRoles = () => [CrmRole.SUPER_ADMIN];
    auth.expireSession = jasmine.createSpy('expireSession');
    const { page } = setup();
    const self = { id: '42', username: 'self', firstName: 'A', lastName: 'B', email: 'self@example.test', phone: null,
      enabled: true, accountNonLocked: true, roles: [CrmRole.SUPER_ADMIN], roleType: CrmRole.SUPER_ADMIN, createdAt: null };
    page.confirmed=true;page.changeRole(self, CrmRole.ADMIN);
    expect(service.setRoles).toHaveBeenCalledWith('42', [CrmRole.ADMIN]);
    expect(auth.expireSession).toHaveBeenCalledOnceWith('/users', 0);
  });
  it('does not clear the current session when changing another account or after a refused change', () => {
    role = CrmRole.SUPER_ADMIN;
    const auth = TestBed.inject(AuthService) as any;
    auth.getCurrentUser = () => ({ username: 'self' }); auth.getRoles = () => [CrmRole.SUPER_ADMIN];
    auth.expireSession = jasmine.createSpy('expireSession');
    const { page } = setup();
    const other = { id: '43', username: 'other', firstName: 'A', lastName: 'B', email: 'other@example.test', phone: null,
      enabled: true, accountNonLocked: true, roles: [CrmRole.ADMIN], roleType: CrmRole.ADMIN, createdAt: null };
    page.confirmed=true;page.changeRole(other, CrmRole.CLIENT); expect(auth.expireSession).not.toHaveBeenCalled();
    service.setRoles.and.returnValue(throwError(() => ({ status: 409, error: { message: 'private diagnostic' } })));
    page.confirmed=true;page.changeRole({ ...other, username: 'self' }, CrmRole.ADMIN);
    expect(auth.expireSession).not.toHaveBeenCalled(); expect(page.error()).not.toContain('private diagnostic');
  });
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
      expect(fixture.nativeElement.textContent).toContain('Associez-le à une fiche correspondante');
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
  it('shows identity, all roles and account flags with constrained role assignment controls', () => {
    service.list.and.returnValue(of([{ id: '42', username: 'multi-role', firstName: 'Test', lastName: 'Account', email: 'test@example.test',
      phone: null, enabled: false, accountNonLocked: false, roleType: CrmRole.ADMIN, roles: [CrmRole.ADMIN, CrmRole.EMPLOYEE], createdAt: null }]));
    const { fixture } = setup(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('multi-role'); expect(text).toContain('ADMIN, EMPLOYEE'); expect(text).toContain('Désactivé'); expect(text).toContain('Verrouillé');
    expect(fixture.nativeElement.textContent).toContain('Action SUPER_ADMIN uniquement');
    expect(fixture.nativeElement.querySelector('select[aria-label="Remplacer les rôles"]')).toBeNull();
  });
  it('allows SUPER_ADMIN to manage administrative account roles', () => {
    role = CrmRole.SUPER_ADMIN;
    service.list.and.returnValue(of([{ id: '42', username: 'admin', firstName: 'A', lastName: 'B', email: 'a@example.test',
      phone: null, enabled: true, accountNonLocked: true, roleType: CrmRole.ADMIN, roles: [CrmRole.ADMIN], createdAt: null }]));
    const { fixture, page } = setup();
    expect(fixture.nativeElement.querySelector('select[aria-label="Remplacer les rôles"]')).not.toBeNull();
    expect(page.selectableRoles()).toContain(CrmRole.SUPER_ADMIN);
  });
  it('creates a CLIENT account and wires profile, role, deactivation and reactivation actions', async () => {
    const { page } = setup(); page.accountRole.set(CrmRole.CLIENT); page.form.setValue(body); page.create();
    expect(service.create).toHaveBeenCalledWith({ ...body, roleType: CrmRole.CLIENT });
    const account = { id: '88', username: 'client', firstName: 'A', lastName: 'B', email: 'a@example.test', phone: null,
      roles: [CrmRole.CLIENT], roleType: CrmRole.CLIENT, enabled: true, accountNonLocked: true, createdAt: null };
    page.confirmed=true;page.changeStatus(account); expect(service.setEnabled).toHaveBeenCalledWith('88', false);
    page.confirmed=true;page.changeRole(account, CrmRole.EMPLOYEE); expect(service.setRoles).toHaveBeenCalledWith('88', [CrmRole.EMPLOYEE]);
    page.selectUser(account); page.editFirstName.set('Updated'); page.saveUser();
    expect(service.update).toHaveBeenCalledWith('88', { firstName: 'Updated', lastName: 'B', email: 'a@example.test', phone: '' });
  });
  it('submits the rendered edit form through a real form event',()=>{
    const {page,fixture}=setup();const user={id:'88',username:'client',firstName:'A',lastName:'B',email:'client@example.invalid',phone:null,roles:[CrmRole.CLIENT],roleType:CrmRole.CLIENT,enabled:true,accountNonLocked:true,createdAt:null};page.selectUser(user);fixture.detectChanges();const form=fixture.nativeElement.querySelector('section[aria-label="Détail et modification du compte"] form');expect(form).not.toBeNull();form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));expect(service.update).toHaveBeenCalledWith('88',{firstName:'A',lastName:'B',email:'client@example.invalid',phone:''});
  });
  it('does not send a role or status write without explicit confirmation',()=>{const {page}=setup();const user={id:'88',username:'client',roles:[CrmRole.CLIENT],enabled:true};page.changeStatus(user);page.changeRole(user,CrmRole.EMPLOYEE);expect(service.setEnabled).not.toHaveBeenCalled();expect(service.setRoles).not.toHaveBeenCalled();});
  function accounts() {
    return Array.from({length:26},(_,i)=>({id:i===0?'9223372036854775807':String(i+1),username:'account-'+i,
      firstName:'Person',lastName:String(i),email:'account-'+i+'@example.test',phone:null,
      roles:[i%2?CrmRole.CLIENT:CrmRole.EMPLOYEE],roleType:CrmRole.EMPLOYEE,
      enabled:i!==0,accountNonLocked:i!==2,createdAt:null}));
  }
  it('requests server pages and preserves long identifiers',()=>{
    const rows=accounts();service.search.and.returnValues(of({content:rows.slice(0,10),number:0,totalElements:26,totalPages:3}),of({content:rows.slice(20),number:2,totalElements:26,totalPages:3}),of({content:[rows[25]],number:0,totalElements:1,totalPages:1}));
    const {page}=setup();expect(page.visibleUsers().length).toBe(10);expect(page.visibleUsers()[0].id).toBe('9223372036854775807');page.paginate({pageIndex:2,pageSize:10});expect(page.visibleUsers().length).toBe(6);expect(service.search).toHaveBeenCalledWith('','','',2,10);page.filter('account-25','','');expect(service.search).toHaveBeenCalledWith('account-25','','',0,10);expect(page.total()).toBe(1);
  });
  it('combines role and state filters on the server and renders no-match state',()=>{
    const {page,fixture}=setup();page.filter('',CrmRole.EMPLOYEE,'locked');expect(service.search).toHaveBeenCalledWith('',CrmRole.EMPLOYEE,'locked',0,10);page.filter('no-match','','');fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Aucun compte ne correspond');expect(page.selectableRoles()).toEqual([CrmRole.EMPLOYEE,CrmRole.CLIENT]);
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
