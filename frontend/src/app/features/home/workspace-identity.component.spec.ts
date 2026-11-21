import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { WorkspaceIdentityComponent } from './workspace-identity.component';
import { AuthService } from '../../core/services/auth.service';

describe('Workspace company information button', () => {
  const url = 'http://localhost:8080/identity/api/users/display-settings';
  const company = { companyName: 'Entreprise de test', supportEmail: 'support@example.test', welcomeMessage: 'Bienvenue', version: 1 };
  const user = signal<object | null>({});
  let generation: number;
  let http: HttpTestingController;
  beforeEach(() => {
    user.set({}); generation = 0;
    TestBed.configureTestingModule({ imports: [WorkspaceIdentityComponent], providers: [
      provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
      { provide: AuthService, useValue: { getCurrentUser: () => user(), get sessionGeneration() { return generation; } } }
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  function setup() {
    const fixture = TestBed.createComponent(WorkspaceIdentityComponent);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    return { fixture, button, element: fixture.nativeElement as HTMLElement };
  }
  it('loads only on click, prevents duplicate requests and never navigates', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate');
    const { fixture, button, element } = setup();
    http.expectNone(() => true);
    expect(button.type).toBe('button');
    expect(button.textContent).toContain('Afficher les informations de l’entreprise');
    button.click(); button.click(); fixture.detectChanges();
    expect(button.disabled).toBeTrue();
    expect(element.querySelector('[role="status"]')).not.toBeNull();
    const request = http.expectOne(url);
    expect(request.request.method).toBe('GET');
    request.flush(company); fixture.detectChanges();
    expect(element.querySelector('h3')?.textContent).toBe(company.companyName);
    expect(element.textContent).toContain(company.welcomeMessage);
    expect(element.textContent).toContain(company.supportEmail);
    expect(button.textContent).toContain('Actualiser les informations');
    expect(button.disabled).toBeFalse();
    expect(navigate).not.toHaveBeenCalled();
  });
  it('refreshes the displayed information and offers retry after failure without stale content or diagnostics', () => {
    const { fixture, button, element } = setup();
    button.click(); http.expectOne(url).flush(company); fixture.detectChanges();
    button.click(); fixture.detectChanges();
    expect(element.querySelector('.company-details')).toBeNull();
    http.expectOne(url).flush({ message: 'PRIVATE SQL diagnostic' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('indisponibles');
    expect(element.textContent).not.toContain('PRIVATE');
    expect(button.textContent).toContain('Réessayer');
    button.click(); http.expectOne(url).flush({ ...company, companyName: 'Organisation actualisée' }); fixture.detectChanges();
    expect(element.querySelector('h3')?.textContent).toBe('Organisation actualisée');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
  it('never requests company information without an authenticated user', () => {
    user.set(null);
    const { button } = setup(); button.click();
    http.expectNone(() => true);
  });
  for (const fail of [false, true]) {
    it(`clears company data and ignores late ${fail ? 'errors' : 'results'} after a session change`, () => {
      const { fixture, button, element } = setup();
      button.click(); http.expectOne(url).flush(company); fixture.detectChanges();
      button.click(); const old = http.expectOne(url);
      generation++; user.set(null); fixture.detectChanges();
      if (fail) old.flush({}, { status: 403, statusText: 'Forbidden' }); else old.flush(company);
      fixture.detectChanges();
      expect(element.querySelector('.company-details')).toBeNull();
      expect(element.querySelector('[role="alert"]')).toBeNull();
      expect(button.disabled).toBeFalse();
    });
  }
  it('renders server text safely and omits an empty support address', () => {
    const { fixture, button, element } = setup();
    button.click(); http.expectOne(url).flush({ ...company, companyName: '<img src=x onerror=alert(1)>', supportEmail: '' });
    fixture.detectChanges();
    expect(element.querySelector('h3')?.textContent).toContain('<img');
    expect(element.querySelector('img')).toBeNull();
    expect(element.querySelector('.support')).toBeNull();
  });
  it('bounds loading to fifteen seconds and allows retry', () => {
    jasmine.clock().install();
    try {
      const { fixture, button, element } = setup(); button.click(); const request = http.expectOne(url);
      jasmine.clock().tick(15001); fixture.detectChanges();
      expect(request.cancelled).toBeTrue();
      expect(button.disabled).toBeFalse();
      expect(element.querySelector('[role="alert"]')).not.toBeNull();
    } finally { jasmine.clock().uninstall(); }
  });
  it('cancels the pending request when leaving the page', () => {
    const { fixture, button } = setup(); button.click(); const request = http.expectOne(url);
    fixture.destroy(); expect(request.cancelled).toBeTrue();
  });
  for (const width of [375, 768, 1440]) {
    it(`keeps the company card and button within a ${width}px viewport`, () => {
      const { fixture, element } = setup();
      const frame = document.createElement('iframe');
      frame.style.cssText = `position:absolute;left:-10000px;width:${width}px;height:600px;border:0;visibility:hidden`;
      document.body.appendChild(frame);
      try {
        const doc = frame.contentDocument!;
        const reset = doc.createElement('style'); reset.textContent = 'html,body{margin:0;width:100%}'; doc.head.appendChild(reset);
        document.querySelectorAll('style').forEach(style => doc.head.appendChild(style.cloneNode(true)));
        doc.body.appendChild(element.cloneNode(true));
        const button = doc.querySelector('button')!;
        expect(doc.documentElement.scrollWidth).toBeLessThanOrEqual(width + 1);
        expect(button.getBoundingClientRect().height).toBeGreaterThanOrEqual(44);
        expect(button.getBoundingClientRect().right).toBeLessThanOrEqual(width);
      } finally { frame.remove(); fixture.destroy(); }
    });
  }
});
