import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { AuthService } from '../../core/services/auth.service';
import { storeTestSession } from '../../core/testing/auth-fixtures';
import { PdfDownloadComponent } from './pdf-download.component';

describe('PDF security with real interceptor and AuthService', () => {
  let http: HttpTestingController;
  let router: Router;
  let auth: AuthService;
  let create: jasmine.Spy;
  let click: jasmine.Spy;
  const refresh = 'http://localhost:8080/identity/api/auth/refresh';
  beforeEach(async () => {
    sessionStorage.clear(); storeTestSession();
    await TestBed.configureTestingModule({ imports: [PdfDownloadComponent], providers: [
      provideZonelessChangeDetection(), provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(), provideRouter([])
    ] }).compileComponents();
    http = TestBed.inject(HttpTestingController); router = TestBed.inject(Router); auth = TestBed.inject(AuthService);
    spyOn(router, 'navigate').and.resolveTo(true);
    create = spyOn(URL, 'createObjectURL').and.returnValue('blob:synthetic-pdf');
    spyOn(URL, 'revokeObjectURL'); click = spyOn(HTMLAnchorElement.prototype, 'click');
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });

  for (const [kind, path] of [['invoice', 'invoice/api/invoices'], ['payroll', 'payroll/api/payrolls'], ['leave', 'hr/api/leaves']]) {
    for (const status of [401, 403, 503]) {
      it(`${kind} HTTP ${status} cannot become a downloaded file`, () => {
        const fixture = TestBed.createComponent(PdfDownloadComponent);
        fixture.componentRef.setInput('kind', kind); fixture.componentRef.setInput('id', 42); fixture.detectChanges();
        const token = auth.getToken();
        fixture.nativeElement.querySelector('button').click();
        const request = http.expectOne(`http://localhost:8080/${path}/42/pdf`);
        expect(request.request.headers.get('Authorization')).toBe('Bearer ' + token);
        request.flush(new Blob(['private diagnostic']), { status, statusText: 'Refused' });
        if (status === 401) http.expectOne(refresh).flush({}, { status: 401, statusText: 'Unauthorized' });
        else http.expectNone(refresh);
        fixture.detectChanges();
        expect(create).not.toHaveBeenCalled(); expect(click).not.toHaveBeenCalled();
        expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
        expect(fixture.nativeElement.textContent).not.toContain('private diagnostic');
        expect(fixture.nativeElement.querySelector('button').disabled).toBeFalse();
        if (status !== 401) { expect(auth.getToken()).toBe(token); expect(router.navigate).not.toHaveBeenCalled(); }
      });
    }
  }

  it('reports unavailable refresh as 503 and keeps PDF retry possible', () => {
    const fixture = TestBed.createComponent(PdfDownloadComponent);
    fixture.componentRef.setInput('kind', 'invoice'); fixture.componentRef.setInput('id', 42); fixture.detectChanges();
    const token = auth.getToken(); fixture.nativeElement.querySelector('button').click();
    http.expectOne('http://localhost:8080/invoice/api/invoices/42/pdf').flush(new Blob(), { status: 401, statusText: 'Unauthorized' });
    http.expectOne(refresh).flush({}, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('temporairement indisponible');
    expect(auth.getToken()).toBe(token); expect(router.navigate).not.toHaveBeenCalled();
    expect(create).not.toHaveBeenCalled(); expect(click).not.toHaveBeenCalled();
  });
});
