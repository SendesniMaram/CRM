import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PdfDownloadComponent } from './pdf-download.component';

describe('PdfDownloadComponent', () => {
  let http: HttpTestingController;
  let create: jasmine.Spy;
  let click: jasmine.Spy;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PdfDownloadComponent], providers: [
      provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()
    ] }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    create = spyOn(URL, 'createObjectURL').and.returnValue('blob:synthetic-pdf-test');
    spyOn(URL, 'revokeObjectURL');
    click = spyOn(HTMLAnchorElement.prototype, 'click');
  });
  afterEach(() => http.verify());
  function setup(kind = 'invoice') {
    const fixture = TestBed.createComponent(PdfDownloadComponent);
    fixture.componentRef.setInput('kind', kind); fixture.componentRef.setInput('id', 42);
    fixture.detectChanges(); return fixture;
  }
  it('requests an authenticated-interceptor-compatible Blob URL and prevents duplicate submissions', () => {
    const fixture = setup(); const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    button.click(); button.click(); fixture.detectChanges();
    expect(button.disabled).toBeTrue(); expect(button.getAttribute('aria-busy')).toBe('true');
    expect(fixture.nativeElement.querySelector('mat-spinner')).toBeTruthy();
    const req = http.expectOne('http://localhost:8080/invoice/api/invoices/42/pdf');
    expect(req.request.method).toBe('GET'); expect(req.request.responseType).toBe('blob');
    expect(create).not.toHaveBeenCalled();
    req.flush(new Blob(['%PDF-1.7 synthetic'], { type: 'application/pdf' }), { headers: { 'Content-Type': 'application/pdf' } });
    fixture.detectChanges();
    expect(create).toHaveBeenCalledTimes(1); expect(click).toHaveBeenCalledTimes(1); expect(button.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });
  it('uses the real HR route for leaves', () => {
    const fixture = setup('leave'); fixture.nativeElement.querySelector('button').click();
    http.expectOne('http://localhost:8080/hr/api/leaves/42/pdf').flush(new Blob(), { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    expect(create).not.toHaveBeenCalled();
  });
  it('rejects a successful JSON response instead of downloading an error document', () => {
    const fixture = setup(); fixture.nativeElement.querySelector('button').click();
    http.expectOne('http://localhost:8080/invoice/api/invoices/42/pdf').flush(new Blob(['{}']), { headers: { 'Content-Type': 'application/json' } });
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy(); expect(create).not.toHaveBeenCalled();
  });
  it('rejects an empty PDF and allows a retry after failure', () => {
    const fixture = setup('payroll'); const button = fixture.nativeElement.querySelector('button'); button.click();
    http.expectOne('http://localhost:8080/payroll/api/payrolls/42/pdf').flush(new Blob(), { headers: { 'Content-Type': 'application/pdf' } });
    fixture.detectChanges(); expect(button.disabled).toBeFalse(); expect(create).not.toHaveBeenCalled();
    button.click(); http.expectOne('http://localhost:8080/payroll/api/payrolls/42/pdf').flush(new Blob(), { status: 500, statusText: 'Error' });
  });
  it('uses a native keyboard-focusable button with an explicit accessible label', () => {
    const fixture = setup(); const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    button.focus(); expect(document.activeElement).toBe(button); expect(button.type).toBe('button');
    expect(button.getAttribute('aria-label')).toContain('PDF');
  });
  for (const [status, message] of [
    [401, 'Session expirée. Veuillez vous reconnecter.'],
    [403, "Vous n'êtes pas autorisé à télécharger ce document."],
    [404, 'Document introuvable.'],
    [500, 'Une erreur serveur est survenue.'],
    [503, 'Le service est temporairement indisponible. Réessayez.']
  ] as const) {
    it(`announces HTTP ${status} without exposing the response body`, () => {
      const fixture = setup(); const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
      button.click();
      http.expectOne('http://localhost:8080/invoice/api/invoices/42/pdf').flush(new Blob(['sensitive error detail']), { status, statusText: 'Error' });
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toBe(message);
      expect(fixture.nativeElement.textContent).not.toContain('sensitive error detail');
      expect(button.disabled).toBeFalse(); expect(create).not.toHaveBeenCalled();
    });
  }
  it('announces a network failure and enables retry', () => {
    const fixture = setup(); fixture.nativeElement.querySelector('button').click();
    http.expectOne('http://localhost:8080/invoice/api/invoices/42/pdf').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toBe('Impossible de contacter le serveur.');
    expect(fixture.nativeElement.querySelector('button').disabled).toBeFalse();
  });
});
