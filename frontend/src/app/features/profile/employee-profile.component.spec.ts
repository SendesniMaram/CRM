import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { EmployeeProfileComponent } from './employee-profile.component';
import { EmployeeProfileService } from './employee-profile.service';

describe('EmployeeProfileComponent', () => {
  let service: jasmine.SpyObj<EmployeeProfileService>; let role: CrmRole; let generation: number;
  const session = signal(0);
  const profile = { employeeId: '103', firstName: 'Test', lastName: 'Employee', jobTitle: 'Developer', departmentId: '7', hireDate: '2026-01-15' };
  beforeEach(() => {
    role = CrmRole.EMPLOYEE; generation = 0;
    service = jasmine.createSpyObj('EmployeeProfileService', ['read']); service.read.and.returnValue(of(profile));
    TestBed.configureTestingModule({ imports: [EmployeeProfileComponent], providers: [provideZonelessChangeDetection(),
      { provide: EmployeeProfileService, useValue: service }, { provide: AuthService, useValue: {
        get sessionGeneration() { return generation; }, getCurrentUser: () => session(), hasAnyRole: (roles: CrmRole[]) => roles.includes(role)
      } }] });
  });
  function setup() { const fixture = TestBed.createComponent(EmployeeProfileComponent); fixture.detectChanges(); return { fixture, page: fixture.componentInstance as any }; }
  it('shows only professional data in a read-only Material card', () => {
    const { fixture } = setup(); const text = fixture.nativeElement.textContent;
    for (const value of ['Mon profil', 'Test', 'Employee', 'Developer', '2026-01-15']) expect(text).toContain(value);
    expect(fixture.nativeElement.querySelector('input, form')).toBeNull(); expect(text).not.toContain('Salaire');
  });
  it('shows loading and blocks duplicate requests', () => {
    const pending = new Subject<any>(); service.read.and.returnValue(pending); const { fixture, page } = setup();
    expect(fixture.nativeElement.textContent).toContain('Chargement du profil'); page.load(); expect(service.read).toHaveBeenCalledTimes(1);
    pending.next(profile); pending.complete(); fixture.detectChanges(); expect(page.loading()).toBeFalse(); expect(page.profile()).toEqual(profile);
  });
  for (const denied of [CrmRole.CLIENT, CrmRole.ADMIN, CrmRole.SUPER_ADMIN]) {
    it('does not expose personal data or fetch for ' + denied, () => {
      role = denied; const { fixture, page } = setup(); page.load(); expect(service.read).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('mat-card')).toBeNull();
    });
  }
  it('explains an absent association without disclosing diagnostics', () => {
    service.read.and.returnValue(throwError(() => ({ status: 404, error: { code: 'PROFILE_NOT_ASSOCIATED', message: 'private diagnostic' } })));
    const { fixture, page } = setup(); expect(fixture.nativeElement.textContent).toContain('pas associé');
    expect(fixture.nativeElement.textContent).not.toContain('private diagnostic'); expect(page.profile()).toBeNull();
  });
  for (const status of [0, 401, 403, 404, 502, 503]) {
    it('handles error ' + status + ' without showing stale profile or server diagnostics', () => {
      const { fixture, page } = setup(); service.read.and.returnValue(throwError(() => ({ status, error: { message: 'private diagnostic' } })));
      page.load(); fixture.detectChanges(); expect(page.profile()).toBeNull(); expect(page.error()).not.toBe('');
      expect(fixture.nativeElement.textContent).not.toContain('private diagnostic');
    });
  }
  it('clears data on logout and cancels an old pending response', () => {
    const pending = new Subject<any>(); service.read.and.returnValue(pending); const { fixture, page } = setup();
    role = CrmRole.CLIENT; generation++; session.update(value => value + 1); fixture.detectChanges();
    pending.next(profile); pending.complete(); fixture.detectChanges(); expect(page.profile()).toBeNull();
    expect(fixture.nativeElement.querySelector('mat-card')).toBeNull();
  });
  it('replaces the profile after an account change, with no old data during loading', () => {
    const { fixture, page } = setup(); expect(page.profile()).toEqual(profile);
    const pending = new Subject<any>(); service.read.and.returnValue(pending); generation++; session.update(value => value + 1); fixture.detectChanges();
    expect(page.profile()).toBeNull(); expect(fixture.nativeElement.textContent).not.toContain('Developer');
    const other = { ...profile, employeeId: '104', firstName: 'Second' }; pending.next(other); pending.complete(); fixture.detectChanges();
    expect(page.profile()).toEqual(other); expect(service.read).toHaveBeenCalledTimes(2);
  });
});
