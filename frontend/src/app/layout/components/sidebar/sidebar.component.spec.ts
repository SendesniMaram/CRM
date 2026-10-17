import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CrmRole } from '../../../core/models/role.model';
import { SidebarComponent } from './sidebar.component';
@Component({standalone: true, template: ''}) class Destination {}
describe('SidebarComponent', () => {
  let auth: jasmine.SpyObj<AuthService>;
  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['hasAnyRole', 'logout']);
    TestBed.configureTestingModule({imports: [SidebarComponent], providers: [
      provideZonelessChangeDetection(), provideRouter([{path:'fees/:id/edit',component:Destination},{path:'leaves',component:Destination}]),
      {provide:AuthService,useValue:auth}
    ]});
  });
  function render(role: CrmRole | null = CrmRole.ADMIN) {
    auth.hasAnyRole.and.callFake(required => role !== null && required.includes(role));
    const fixture = TestBed.createComponent(SidebarComponent); fixture.detectChanges(); return fixture;
  }
  for (const role of [CrmRole.ADMIN,CrmRole.SUPER_ADMIN,CrmRole.EMPLOYEE,CrmRole.CLIENT]) {
    it('filters all navigation entries for '+role, () => {
      const fixture=render(role); const element=fixture.nativeElement as HTMLElement;
      expect(element.querySelector('a[href="/home"]')).not.toBeNull();
      expect(element.querySelector('a[href="/dashboard"]')).not.toBeNull();
      for(const path of ['/employees','/departments','/customers','/invoices','/fees','/payroll']) {
        expect(!!element.querySelector('a[href="'+path+'"]')).toBe([CrmRole.ADMIN,CrmRole.SUPER_ADMIN].includes(role));
      }
      expect(!!element.querySelector('a[href="/leaves"]')).toBe(role!==CrmRole.CLIENT);
      expect(!!element.querySelector('a[href="/leaves#associations"]')).toBe([CrmRole.ADMIN,CrmRole.SUPER_ADMIN].includes(role));
      expect(element.querySelector('a[href="/employee-area"],a[href="/client-area"]')).toBeNull();
    });
  }
  it('groups entries and retains accessible labels when collapsed', () => {
    const fixture=render(); fixture.componentRef.setInput('collapsed',true); fixture.detectChanges();
    const nav=fixture.nativeElement.querySelector('nav');
    expect(nav.getAttribute('aria-label')).toBe('Navigation principale');
    expect(nav.querySelectorAll('section').length).toBe(4);
    const link=nav.querySelector('a[href="/employees"]');
    expect(link.getAttribute('aria-label')).toBe('Employés');
    expect(link.querySelector('span').classList.contains('visually-hidden')).toBeTrue();
    expect(fixture.nativeElement.querySelector('.compact')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.collapse-toggle').getAttribute('aria-expanded')).toBe('false');
  });
  it('emits the collapse action and hides it on mobile', () => {
    const fixture=render(); const spy=jasmine.createSpy();fixture.componentInstance.toggleCollapse.subscribe(spy);
    fixture.nativeElement.querySelector('.collapse-toggle').click(); expect(spy).toHaveBeenCalledTimes(1);
    fixture.componentRef.setInput('mobile',true);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.collapse-toggle')).toBeNull();
  });
  it('marks a parent module active on detail/edit routes', async () => {
    const fixture=render(); await TestBed.inject(Router).navigateByUrl('/fees/7/edit');await fixture.whenStable();fixture.detectChanges();
    const active=fixture.nativeElement.querySelector('a[href="/fees"]');
    expect(active.classList.contains('active')).toBeTrue();expect(active.getAttribute('aria-current')).toBe('page');
  });
  it('activates the association link only for the exact fragment', async () => {
    const fixture=render(); const router=TestBed.inject(Router);
    await router.navigateByUrl('/leaves');await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/leaves#associations"]').classList.contains('active')).toBeFalse();
    await router.navigateByUrl('/leaves#associations');await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/leaves#associations"]').classList.contains('active')).toBeTrue();
    expect(fixture.nativeElement.querySelector('a[href="/leaves"]').classList.contains('active')).toBeFalse();
    expect(fixture.nativeElement.querySelectorAll('a[aria-current="page"]').length).toBe(1);
  });
  it('emits navigation and account actions and keeps logout', () => {
    const fixture=render();const navigate=jasmine.createSpy();const account=jasmine.createSpy();
    fixture.componentInstance.navigate.subscribe(navigate);fixture.componentInstance.account.subscribe(account);
    fixture.nativeElement.querySelector('a').click();expect(navigate).toHaveBeenCalledTimes(1);
    fixture.nativeElement.querySelector('button[aria-label="Mon compte"]').click();expect(account).toHaveBeenCalledTimes(1);
    fixture.nativeElement.querySelector('button[aria-label="Se déconnecter"]').click();expect(auth.logout).toHaveBeenCalledTimes(1);
  });
  it('exposes no module or Dashboard shortcut for an unknown role', () => {
    const fixture=render(null);expect(fixture.nativeElement.querySelectorAll('nav a').length).toBe(0);
  });
});
