import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { WorkspaceClock } from '../../../core/navigation/workspace-clock';
import { NavbarComponent } from './navbar.component';
import { AccountDialogComponent } from './account-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../../core/services/auth.service';
import { CrmRole } from '../../../core/models/role.model';
@Component({standalone:true,template:''}) class Destination {}
describe('Navbar and account information', () => {
  const role=signal(CrmRole.EMPLOYEE);
  beforeEach(() => {role.set(CrmRole.EMPLOYEE);TestBed.configureTestingModule({imports:[NavbarComponent,AccountDialogComponent],providers:[
    provideZonelessChangeDetection(),provideRouter([{path:'leaves/:id/edit',component:Destination}]),{provide:AuthService,useValue:{
      hasAnyRole:(roles:CrmRole[])=>roles.includes(role()),getCurrentUser:()=>({username:'unit-user',email:'unit@example.invalid'}),logout:jasmine.createSpy('logout')
    }}]});});
  it('uses the current module and actual role instead of a static Administration label', async () => {
    const fixture=TestBed.createComponent(NavbarComponent);fixture.detectChanges();await TestBed.inject(Router).navigateByUrl('/leaves/7/edit');fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.page-title').textContent).toBe('Congés');expect(fixture.nativeElement.querySelector('.role').textContent).toBe('Employé');
    expect(fixture.nativeElement.textContent).not.toContain('Administration');
  });
  it('exposes mobile navigation state and emits its trigger action', () => {
    const fixture=TestBed.createComponent(NavbarComponent);fixture.componentRef.setInput('mobile',true);fixture.componentRef.setInput('navigationOpen',true);fixture.detectChanges();
    const spy=jasmine.createSpy();fixture.componentInstance.toggleNavigation.subscribe(spy);const button=fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]');
    expect(button.getAttribute('aria-expanded')).toBe('true');button.click();expect(spy).toHaveBeenCalledTimes(1);
  });
  it('provides contextual breadcrumbs only for recognized nested routes', async () => {
    const fixture=TestBed.createComponent(NavbarComponent);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.breadcrumb')).toBeNull();
    await TestBed.inject(Router).navigateByUrl('/leaves/7/edit');fixture.detectChanges();
    const nav=fixture.nativeElement.querySelector('nav[aria-label="Fil d’Ariane"]');
    expect(nav.querySelector('a').getAttribute('href')).toBe('/leaves');
    expect(nav.querySelector('[aria-current="page"]').textContent).toBe('Modification');
  });
  it('shows the account menu, emits account selection and logs out through AuthService', async () => {
    const fixture=TestBed.createComponent(NavbarComponent);fixture.detectChanges();
    const selected=jasmine.createSpy();fixture.componentInstance.account.subscribe(selected);
    const trigger=fixture.nativeElement.querySelector('button[aria-label="Ouvrir le menu du compte"]');
    expect(trigger.getAttribute('aria-haspopup')).toBe('menu');
    expect(fixture.nativeElement.querySelector('.avatar').textContent.trim()).toBe('UU');
    trigger.click();fixture.detectChanges();await fixture.whenStable();
    document.querySelector<HTMLButtonElement>('button[aria-label="Consulter mon compte"]')!.click();
    expect(selected).toHaveBeenCalledTimes(1);fixture.detectChanges();await fixture.whenStable();
    trigger.click();fixture.detectChanges();await fixture.whenStable();
    const logout=Array.from(document.querySelectorAll<HTMLButtonElement>('button[mat-menu-item]')).find(button=>button.textContent?.includes('Se déconnecter'))!;
    expect(document.querySelector('mat-menu')?.textContent).not.toContain('Paramètres');
    logout.click();expect(TestBed.inject(AuthService).logout).toHaveBeenCalledTimes(1);
  });
  it('exposes the desktop sidebar collapse state and formats the current French date', () => {
    TestBed.inject(WorkspaceClock).now.set(new Date(2026,9,16,10));
    const fixture=TestBed.createComponent(NavbarComponent);fixture.componentRef.setInput('collapsed',true);fixture.detectChanges();
    const trigger=fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');expect(trigger.getAttribute('aria-label')).toBe('Développer la navigation');
    expect(fixture.nativeElement.querySelector('time').textContent).toBe('vendredi 16 octobre 2026');
    expect(fixture.nativeElement.querySelector('.username').textContent).toContain('unit-user');
  });
  it('closes the account menu with Escape and returns focus to its trigger', async () => {
    const fixture=TestBed.createComponent(NavbarComponent);fixture.detectChanges();
    const trigger=fixture.nativeElement.querySelector('button[aria-label="Ouvrir le menu du compte"]') as HTMLButtonElement;
    trigger.click();fixture.detectChanges();await fixture.whenStable();
    const focus=spyOn(trigger,'focus').and.callThrough();
    document.querySelector('[role="menu"]')!.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,bubbles:true}));
    fixture.detectChanges();await fixture.whenStable();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');expect(focus).toHaveBeenCalled();
  });
  it('shows only public session information in an accessible account dialog', async () => {
    const opener=TestBed.createComponent(NavbarComponent);opener.detectChanges();const dialog=TestBed.inject(MatDialog).open(AccountDialogComponent);await opener.whenStable();
    const text=document.querySelector('mat-dialog-container')?.textContent??'';
    expect(text).toContain('unit-user');expect(text).toContain('unit@example.invalid');expect(text).toContain('Employé');
    expect(text).not.toContain('token');expect(text).not.toContain('password');dialog.close();await opener.whenStable();
  });
});
