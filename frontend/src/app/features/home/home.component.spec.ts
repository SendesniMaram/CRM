import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { MatDialog } from '@angular/material/dialog';
import { AccountDialogComponent } from '../../layout/components/navbar/account-dialog.component';
import { WorkspaceClock } from '../../core/navigation/workspace-clock';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  const role=signal<CrmRole | null>(CrmRole.ADMIN);
  beforeEach(() => { role.set(CrmRole.ADMIN);TestBed.configureTestingModule({imports:[HomeComponent],providers:[
    provideZonelessChangeDetection(),provideRouter([]),{provide:AuthService,useValue:{
      getCurrentUser:()=>({username:'Compte de test',email:'unit@example.invalid'}),
      hasAnyRole:(required:CrmRole[])=>role()!==null && required.includes(role()!)
    }}]}); });
  for(const current of [CrmRole.SUPER_ADMIN,CrmRole.ADMIN,CrmRole.EMPLOYEE,CrmRole.CLIENT]) {
    it('renders real session identity and only authorized modules for '+current, () => {
      role.set(current);const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;
      expect(element.querySelector('h1')?.textContent).toContain('Compte de test');
      expect(element.querySelectorAll('.module-card').length).toBe(current===CrmRole.CLIENT?2:current===CrmRole.EMPLOYEE?4:18);
      const actions=Array.from(element.querySelectorAll('.action-links a')).map(link=>link.getAttribute('href'));
      if(current===CrmRole.EMPLOYEE)expect(actions).toEqual(['/leaves/new','/leaves']);
      else if(current===CrmRole.CLIENT)expect(actions).toEqual([]);
      else {expect(actions.length).toBe(7);expect(actions).not.toContain('/leaves/new');}
      expect(element.querySelector('.dashboard-link a')?.getAttribute('href')).toBe('/dashboard');
      expect(element.textContent).not.toContain('30 583');expect(element.querySelector('.kpis')).toBeNull();
    });
  }
  it('shows functional guidance, real identity, role and a dynamic year without mock statistics', () => {
    const clock=TestBed.inject(WorkspaceClock);clock.now.set(new Date(2027,0,1,19));
    const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Bonsoir, Compte de test');
    expect(element.querySelector('.identity')?.textContent).toContain('Administrateur');
    expect(element.querySelector('footer')?.textContent).toContain('2027');
    expect(element.querySelectorAll('.guide li').length).toBe(3);
    expect(element.textContent).toContain('Découvrez votre espace CRM');
    expect(element.textContent).toContain('Votre espace sécurisé');
    const card=Array.from(element.querySelectorAll('.module-card')).find(link=>link.getAttribute('href')==='/invoices')!;
    expect(card.textContent).toContain('montants');expect(card.textContent).toContain('statuts');
  });
  it('offers a working account action for clients without promising administrative data', () => {
    role.set(CrmRole.CLIENT);const dialog=spyOn(TestBed.inject(MatDialog),'open');
    const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.hero-actions button')!.click();
    expect(dialog).toHaveBeenCalledWith(AccountDialogComponent,jasmine.objectContaining({ariaLabel:'Informations du compte'}));
    expect(element.querySelector('.module-card')?.textContent).toContain('sans indicateurs administratifs');
    expect(element.querySelector('.intro')?.textContent).not.toContain('facturation');
    expect(element.querySelector('a[href="/employees/new"]')).toBeNull();
  });
  it('keeps employee descriptions and actions within personal leave permissions', () => {
    role.set(CrmRole.EMPLOYEE);const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;
    expect(element.querySelector('a.module-card[href="/leaves"]')?.textContent).toContain('uniquement vos demandes');
    expect(element.querySelector('.action-links')?.textContent).toContain('Consulter mes congés');
    expect(element.querySelector('.intro')?.textContent).not.toContain('facturation');
    expect(element.querySelector('a[href="/payroll"]')).toBeNull();
  });
  it('clears admin links when roles change without retaining a previous role snapshot', () => {
    const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();expect(fixture.nativeElement.querySelector('a[href="/payroll"]')).not.toBeNull();
    role.set(CrmRole.CLIENT);fixture.detectChanges();expect(fixture.nativeElement.querySelector('a[href="/payroll"]')).toBeNull();expect(fixture.nativeElement.querySelector('a[href="/client"]')).not.toBeNull();
  });
  it('provides no Dashboard shortcut to an unrecognized role', () => {
    role.set(null);const fixture=TestBed.createComponent(HomeComponent);fixture.detectChanges();expect(fixture.nativeElement.querySelector('.dashboard-link')).toBeNull();
  });
});
