import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CrmRole } from '../../../core/models/role.model';
import { SidebarComponent } from './sidebar.component';

describe('SidebarComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        provideRouter([]),
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();
  });

  function renderFor(roles: CrmRole[]): string {
    authService.hasAnyRole.and.callFake((requiredRoles) => requiredRoles.some((role) => roles.includes(role)));
    const fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();
    return fixture.nativeElement.textContent;
  }

  it('shows the dashboard for an authenticated user', () => {
    expect(renderFor([CrmRole.ADMIN])).toContain('Tableau de bord');
  });

  it('shows Employees to an ADMIN', () => {
    expect(renderFor([CrmRole.ADMIN])).toContain('Employés');
  });

  it('does not show Employees to an EMPLOYEE', () => {
    expect(renderFor([CrmRole.EMPLOYEE])).not.toContain('Employés');
  });

  it('does not show Payroll to a CLIENT', () => {
    expect(renderFor([CrmRole.CLIENT])).not.toContain('Paie');
  });

  it('shows Employee area to an EMPLOYEE', () => {
    expect(renderFor([CrmRole.EMPLOYEE])).toContain('Espace employé');
  });

  it('shows Client area to a CLIENT', () => {
    expect(renderFor([CrmRole.CLIENT])).toContain('Espace client');
  });
});