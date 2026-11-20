import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { LandingComponent } from './landing.component';
import { routes } from '../../app.routes';
import { AuthService } from '../../core/services/auth.service';
import { CrmNavigation, CRM_NAVIGATION } from '../../core/navigation/crm-navigation';

describe('Public presentation', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter(routes), provideHttpClient(), provideHttpClientTesting()] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); sessionStorage.clear(); });

  it('opens welcome without authentication or any private data request', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/welcome', LandingComponent);
    expect(TestBed.inject(Router).url).toBe('/welcome');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('clarté');
    TestBed.inject(HttpTestingController).expectNone(() => true);
  });

  it('presents seven real modules and labels the dashboard as fictitious', async () => {
    const harness = await RouterTestingHarness.create('/welcome');
    expect(harness.routeNativeElement?.querySelectorAll('.module-card').length).toBe(8);
    for (const name of ['Employés', 'Départements', 'Congés', 'Clients', 'Factures', 'Honoraires', 'Paie']) {
      expect(harness.routeNativeElement?.textContent).toContain(name);
    }
    expect(harness.routeNativeElement?.querySelector('figcaption')?.textContent).toContain('Données fictives');
    expect(harness.routeNativeElement?.querySelector('a[href="/welcome#fonctionnalites"]')).not.toBeNull();
  });

  it('expands FAQ with native keyboard-accessible disclosure controls', async () => {
    const harness = await RouterTestingHarness.create('/welcome');
    const details = harness.routeNativeElement!.querySelector<HTMLDetailsElement>('app-landing-faq details')!;
    expect(details.open).toBeFalse();
    details.querySelector('summary')!.click();
    expect(details.open).toBeTrue();
    details.querySelector('summary')!.click();
    expect(details.open).toBeFalse();
  });

  it('offers working login links on every public module card for a guest', async () => {
    const harness = await RouterTestingHarness.create('/welcome');
    const cards = harness.routeNativeElement!.querySelectorAll<HTMLAnchorElement>('a.module-card');
    expect(cards.length).toBe(8);
    expect([...cards].every(card => card.getAttribute('href') === '/login')).toBeTrue();
  });

  it('links only permitted modules directly and keeps other cards on the user workspace', async () => {
    spyOn(TestBed.inject(AuthService), 'isAuthenticated').and.returnValue(true);
    spyOn(TestBed.inject(CrmNavigation), 'items').and.returnValue(
      CRM_NAVIGATION.filter(item => ['/home', '/dashboard', '/leaves'].includes(item.route))
    );
    const harness = await RouterTestingHarness.create('/welcome');
    const cards = [...harness.routeNativeElement!.querySelectorAll<HTMLAnchorElement>('a.module-card')];
    expect(cards.find(card => card.textContent?.includes('Congés'))?.getAttribute('href')).toBe('/leaves');
    expect(cards.find(card => card.textContent?.includes('Employés'))?.getAttribute('href')).toBe('/home');
    expect(cards.at(-1)?.getAttribute('href')).toBe('/dashboard');
  });

  it('navigates from the public CTA to login and keeps home protected', async () => {
    const harness = await RouterTestingHarness.create('/welcome');
    harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.hero a[href="/login"]')!.click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login');
    await harness.navigateByUrl('/home');
    expect(TestBed.inject(Router).url).toBe('/login?returnUrl=%2Fhome');
  });
});
