import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CrmRole } from '../../../core/models/role.model';
import { SidebarComponent } from './sidebar.component';

describe('SidebarComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole', 'logout']);
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

  it('provides an accessible logout action', () => {
    authService.hasAnyRole.and.returnValue(true);
    const fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.textContent).toContain('Se déconnecter');
    button.click();
    expect(authService.logout).toHaveBeenCalledTimes(1);
  });

  for (const role of [CrmRole.SUPER_ADMIN, CrmRole.ADMIN, CrmRole.EMPLOYEE]) {
    it(`groups a single Leave link under a non-navigable HR heading for ${role}`, () => {
      authService.hasAnyRole.and.callFake(required => required.includes(role));
      const fixture = TestBed.createComponent(SidebarComponent);
      fixture.detectChanges();
      const section: HTMLElement = fixture.nativeElement.querySelector('section[aria-labelledby="hr-navigation-title"]');
      expect(section.querySelector('h3')?.textContent).toBe('Ressources humaines');
      expect(section.querySelectorAll('a').length).toBe(role === CrmRole.EMPLOYEE ? 1 : 2);
      expect(section.querySelector('a')?.getAttribute('href')).toBe('/leaves');
      expect(fixture.nativeElement.querySelectorAll('a[href="/leaves"]').length).toBe(1);
      expect(fixture.nativeElement.querySelector('a[href="/hr"]')).toBeNull();
      expect(section.textContent).not.toContain('Formations');
      expect(section.textContent).not.toContain('Évaluations');
      expect(section.textContent).not.toContain('Recrutement');
    });
  }

  it('hides the HR group from CLIENT', () => {
    const text = renderFor([CrmRole.CLIENT]);
    expect(text).not.toContain('Ressources humaines');
    expect(text).not.toContain('Congés');
  });

  for (const role of [CrmRole.SUPER_ADMIN, CrmRole.ADMIN, CrmRole.EMPLOYEE, CrmRole.CLIENT]) {
    it(`does not expose unimplemented destinations for ${role}`, () => {
      authService.hasAnyRole.and.callFake(required => required.includes(role));
      const fixture = TestBed.createComponent(SidebarComponent);
      fixture.detectChanges();
      for (const path of ['/customers', '/payroll', '/fees', '/invoices', '/employee-area', '/client-area']) {
        expect(fixture.nativeElement.querySelector(`a[href="${path}"]`)).toBeNull();
      }
      expect(fixture.nativeElement.textContent).toContain('Non disponible');
      const link = fixture.nativeElement.querySelector('a[href="/leaves#associations"]');
      if (role === CrmRole.ADMIN || role === CrmRole.SUPER_ADMIN) expect(link).toBeTruthy();
      else expect(link).toBeNull();
    });
  }
});
