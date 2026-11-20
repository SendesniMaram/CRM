import { ANIMATION_MODULE_TYPE, Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { By } from '@angular/platform-browser';
import { MatSidenav } from '@angular/material/sidenav';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject } from 'rxjs';
import { LayoutComponent } from './layout.component';
import { AuthService } from '../../../core/services/auth.service';
import { AccountDialogComponent } from '../navbar/account-dialog.component';
@Component({standalone:true,template:''}) class Destination {}

describe('responsive LayoutComponent', () => {
  let screen:BehaviorSubject<BreakpointState>;
  let dialog:jasmine.SpyObj<MatDialog>;
  beforeEach(() => {
    screen=new BehaviorSubject<BreakpointState>({matches:false,breakpoints:{}});dialog=jasmine.createSpyObj('MatDialog',['open']);
    TestBed.configureTestingModule({imports:[LayoutComponent],providers:[provideZonelessChangeDetection(),{provide:ANIMATION_MODULE_TYPE,useValue:'NoopAnimations'},provideRouter([{path:'home',component:Destination}]),
      {provide:BreakpointObserver,useValue:{observe:()=>screen.asObservable()}},{provide:MatDialog,useValue:dialog},
      {provide:AuthService,useValue:{getCurrentUser:()=>({username:'unit',email:'unit@example.invalid'}),hasAnyRole:()=>true,logout:()=>{}}}
    ]});
  });
  async function create(mobile=false) {
    screen.next({matches:mobile,breakpoints:{}});const fixture=TestBed.createComponent(LayoutComponent);fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();return fixture;
  }
  async function settle() {
    // Material dispatches animation completion, then its async EventEmitter.
    for(let turn=0;turn<3;turn++) await new Promise(resolve=>setTimeout(resolve,0));
  }
  it('opens a desktop side panel and switches to a compact icon rail', async () => {
    const fixture=await create();const drawer=fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    expect(drawer.mode).toBe('side');expect(drawer.opened).toBeTrue();
    fixture.nativeElement.querySelector('.collapse-toggle').click();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-sidenav').classList.contains('compact')).toBeTrue();
    fixture.nativeElement.querySelector('.collapse-toggle').click();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-sidenav').classList.contains('compact')).toBeFalse();
  });
  it('collapses and expands the desktop sidebar from the navbar', async () => {
    const fixture=await create();const trigger=fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]');
    trigger.click();fixture.detectChanges();expect(fixture.nativeElement.querySelector('mat-sidenav').classList.contains('compact')).toBeTrue();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    trigger.click();fixture.detectChanges();expect(fixture.nativeElement.querySelector('mat-sidenav').classList.contains('compact')).toBeFalse();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
  });
  it('starts mobile closed, opens via navbar and closes after a selection with restored focus', async () => {
    const fixture=await create(true);const drawer=fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    expect(drawer.mode).toBe('over');expect(drawer.opened).toBeFalse();
    const trigger=fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]') as HTMLButtonElement;
    trigger.click();fixture.detectChanges();await fixture.whenStable();expect(drawer.opened).toBeTrue();
    const focusSpy=spyOn(trigger,'focus').and.callThrough();
    fixture.nativeElement.querySelector('app-sidebar a').click();fixture.detectChanges();await fixture.whenStable();await settle();fixture.detectChanges();
    expect(drawer.opened).toBeFalse();expect(focusSpy).toHaveBeenCalled();
  });
  it('closes the mobile drawer with Escape', async () => {
    const fixture=await create(true);const drawer=fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]').click();fixture.detectChanges();await fixture.whenStable();
    fixture.nativeElement.querySelector('mat-sidenav').dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,bubbles:true}));
    fixture.detectChanges();await fixture.whenStable();await settle();fixture.detectChanges();expect(drawer.opened).toBeFalse();expect(fixture.componentInstance['mobileOpen']()).toBeFalse();
  });
  it('adapts to a screen-size change without reopening the mobile drawer', async () => {
    const fixture=await create();screen.next({matches:true,breakpoints:{}});fixture.detectChanges();await fixture.whenStable();
    const drawer=fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    expect(drawer.mode).toBe('over');expect(drawer.opened).toBeFalse();
    screen.next({matches:false,breakpoints:{}});fixture.detectChanges();await fixture.whenStable();expect(drawer.mode).toBe('side');expect(drawer.opened).toBeTrue();
  });
  it('opens account information from the navbar and provides a skip link', async () => {
    const fixture=await create();fixture.nativeElement.querySelector('button[aria-label="Ouvrir le menu du compte"]').click();fixture.detectChanges();await fixture.whenStable();document.querySelector<HTMLButtonElement>('button[aria-label="Consulter mon compte"]')!.click();
    expect(dialog.open).toHaveBeenCalledWith(AccountDialogComponent,jasmine.objectContaining({ariaLabel:'Informations du compte'}));
    expect(fixture.nativeElement.querySelector('.skip-link').getAttribute('href')).toBe('#crm-content');
    expect(fixture.nativeElement.querySelector('#crm-content').getAttribute('tabindex')).toBe('-1');
  });
  it('waits for mobile drawer closure before opening the account dialog', async () => {
    const fixture=await create(true);fixture.nativeElement.querySelector('button[aria-controls="crm-drawer"]').click();fixture.detectChanges();await fixture.whenStable();
    fixture.nativeElement.querySelector('app-sidebar button[aria-label="Mon compte"]').click();fixture.detectChanges();await fixture.whenStable();await settle();fixture.detectChanges();
    expect(fixture.componentInstance['mobileOpen']()).toBeFalse();expect(dialog.open).toHaveBeenCalledTimes(1);
  });

  it('moves skip-link focus to content without navigating through the base URL', async () => {
    const fixture = await create();
    const main = fixture.nativeElement.querySelector('#crm-content');
    const focus = spyOn(main, 'focus');
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });
    fixture.nativeElement.querySelector('.skip-link').dispatchEvent(event);
    expect(event.defaultPrevented).toBeTrue();
    expect(focus).toHaveBeenCalled();
  });
});
