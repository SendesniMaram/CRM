import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { EmployeeAssociationComponent } from './employee-association.component';
import { EmployeeAssociationService } from './employee-association.service';

describe('EmployeeAssociationComponent', () => {
  let service: jasmine.SpyObj<EmployeeAssociationService>;
  let role: CrmRole; let generation: number;
  const session = signal({ revision: 0 });
  const options = { users: [{ id: '42', label: 'employee-a' }, { id: '99', label: 'employee-b' }], employees: [{ id: '103', label: 'EMP-103 — Test Employee' }] };
  const link = { userId: '42', employeeId: '103' };
  beforeEach(() => {
    role = CrmRole.ADMIN; generation = 0;
    service = jasmine.createSpyObj('EmployeeAssociationService', ['options', 'read', 'create']);
    service.options.and.returnValue(of(options)); service.read.and.returnValue(throwError(() => ({ status: 404 })));
    service.create.and.returnValue(of({ association: link, created: true }));
    TestBed.configureTestingModule({ imports: [EmployeeAssociationComponent], providers: [provideZonelessChangeDetection(),
      { provide: EmployeeAssociationService, useValue: service }, { provide: AuthService, useValue: {
        get sessionGeneration() { return generation; }, getCurrentUser: () => session(),
        hasAnyRole: (roles: CrmRole[]) => roles.includes(role)
      } }] });
  });
  function setup() {
    const fixture = TestBed.createComponent(EmployeeAssociationComponent); fixture.detectChanges();
    return { fixture, page: fixture.componentInstance as any };
  }
  function choose(page: any) { page.load(); page.user.setValue('42'); page.select(); page.employee.setValue('103'); }
  for (const admin of [CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it('allows explicit association for ' + admin, () => {
      role = admin; const { fixture, page } = setup(); choose(page); fixture.detectChanges();
      expect(page.state()).toBe('absent'); expect(fixture.nativeElement.textContent).toContain('pas d’association');
      page.create(); expect(service.create).toHaveBeenCalledOnceWith('42', '103'); expect(page.link()).toEqual(link);
      fixture.destroy();
    });
  }
  for (const denied of [CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it('hides administration and makes no request for ' + denied, () => {
      role = denied; const { fixture, page } = setup(); page.load(); page.read(); page.create();
      expect(fixture.nativeElement.querySelector('section')).toBeNull(); expect(service.options).not.toHaveBeenCalled();
      expect(service.read).not.toHaveBeenCalled(); expect(service.create).not.toHaveBeenCalled();
    });
  }
  it('shows the associated Employee and has no reassignment or deletion control', () => {
    service.read.and.returnValue(of(link)); const { fixture, page } = setup(); choose(page); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('EMP-103'); expect(page.state()).toBe('linked');
    expect(fixture.nativeElement.querySelector('form')).toBeNull(); page.create(); expect(service.create).not.toHaveBeenCalled();
  });
  it('requires explicit loaded references and a confirmed absence', () => {
    const { page } = setup(); page.user.setValue('42'); page.employee.setValue('103'); page.create(); expect(service.create).not.toHaveBeenCalled();
    choose(page); page.employee.setValue('999'); page.create(); expect(service.create).not.toHaveBeenCalled();
    page.employee.setValue('103'); page.user.setValue('999'); page.create(); expect(service.create).not.toHaveBeenCalled();
  });
  it('keeps independently selected long identifiers as strings throughout the form', () => {
    const longLink = { userId: '9007199254740993', employeeId: '9223372036854775807' };
    service.options.and.returnValue(of({ users: [{ id: longLink.userId, label: 'long-account' }],
      employees: [{ id: longLink.employeeId, label: 'long-employee' }] }));
    service.create.and.returnValue(of({ association: longLink, created: true }));
    const { fixture, page } = setup(); page.load(); page.user.setValue(longLink.userId); page.select();
    page.employee.setValue(longLink.employeeId); page.create();
    expect(service.create).toHaveBeenCalledOnceWith(longLink.userId, longLink.employeeId);
    expect(page.link()).toEqual(longLink); fixture.destroy();
  });
  it('blocks double submission and reloads the authoritative state after success', async () => {
    const pending = new Subject<any>(); service.create.and.returnValue(pending);
    const { fixture, page } = setup(); choose(page); page.create(); page.create(); page.load(); page.select();
    expect(service.create).toHaveBeenCalledTimes(1); expect(service.options).toHaveBeenCalledTimes(1);
    expect(page.user.disabled).toBeTrue(); expect(page.employee.disabled).toBeTrue();
    service.read.and.returnValue(of(link)); pending.next({ association: link, created: true }); pending.complete();
    await fixture.whenStable(); expect(service.read).toHaveBeenCalledTimes(2); expect(page.link()).toEqual(link);
    expect(page.notice()).toBe('Association créée.'); page.create(); expect(service.create).toHaveBeenCalledTimes(1);
  });
  it('distinguishes an idempotent repeated creation', async () => {
    service.create.and.returnValue(of({ association: link, created: false }));
    const { fixture, page } = setup(); choose(page); service.read.and.returnValue(of(link)); page.create();
    await fixture.whenStable(); expect(page.notice()).toContain('déjà enregistrée'); expect(page.link()).toEqual(link);
  });
  for (const status of [400, 401, 403, 404, 409, 502, 503]) {
    it('safely reports mutation failure ' + status + ' and requires rereading before retry', () => {
      service.create.and.returnValue(throwError(() => ({ status, error: { message: 'private diagnostics' } })));
      const { page } = setup(); choose(page); page.create(); page.create();
      expect(service.create).toHaveBeenCalledTimes(1); expect(page.notice()).toBe(''); expect(page.state()).toBe('unknown');
      expect(page.error()).not.toBe(''); expect(page.error()).not.toContain('private diagnostics');
      expect(page.employee.value).toBe('103'); expect(page.user.value).toBe('42');
      if (status === 409) expect(page.error()).toContain('Conflit');
    });
  }
  for (const status of [400, 401, 403, 409, 502, 503]) {
    it('does not interpret a failed read ' + status + ' as a missing association', () => {
      service.read.and.returnValue(throwError(() => ({ status })));
      const { page } = setup(); choose(page); page.create();
      expect(page.state()).toBe('unknown'); expect(service.create).not.toHaveBeenCalled();
    });
  }
  it('does not reuse state when another account is selected', () => {
    service.read.and.returnValue(of(link)); const { page } = setup(); choose(page);
    service.read.and.returnValue(throwError(() => ({ status: 503 }))); page.user.setValue('99'); page.select();
    expect(page.link()).toBeNull(); expect(page.employee.value).toBe(''); expect(page.state()).toBe('unknown');
  });
  it('discards a late read response after session replacement', () => {
    const pending = new Subject<any>(); service.read.and.returnValue(pending);
    const { page } = setup(); page.load(); page.user.setValue('42'); page.select(); generation++;
    pending.next(link); pending.complete(); expect(page.link()).toBeNull(); expect(page.options()).toBeNull();
    expect(page.busy()).toBeFalse();
  });
  it('clears already displayed personal references on logout even without a request', () => {
    service.read.and.returnValue(of(link)); const { fixture, page } = setup(); choose(page); fixture.detectChanges();
    generation++; role = CrmRole.CLIENT; session.update(value => ({ revision: value.revision + 1 })); fixture.detectChanges();
    expect(page.link()).toBeNull(); expect(page.options()).toBeNull(); expect(page.user.value).toBe('');
    expect(fixture.nativeElement.querySelector('section')).toBeNull();
  });
  it('discards a late successful mutation and its queued refresh after a session change', async () => {
    const pending = new Subject<any>(); service.create.and.returnValue(pending);
    const { fixture, page } = setup(); choose(page); page.create(); generation++;
    pending.next({ association: link, created: true }); pending.complete(); await fixture.whenStable();
    expect(page.notice()).toBe(''); expect(page.link()).toBeNull(); expect(service.read).toHaveBeenCalledTimes(1);
  });
});
