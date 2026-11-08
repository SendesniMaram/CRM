import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { PayrollService } from './payroll.service';

describe('PayrollService', () => {
  let service: PayrollService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/payroll/api/payrolls';
  const request = { employeeId: '7', baseSalary: 2000, bonuses: 100, deductions: 50, taxAmount: 200, payPeriod: '2026-10', paymentDate: '2026-10-31', currency: 'TND', status: 'PENDING' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PayrollService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('uses server paging and supported sorting/search through Gateway', () => {
    service.list({ page: 2, size: 25, sortBy: 'status', direction: 'desc', keyword: ' test ' }).subscribe();
    const call = http.expectOne(r => r.url === url);
    expect(call.request.method).toBe('GET');
    expect(call.request.params.get('page')).toBe('2');
    expect(call.request.params.get('size')).toBe('25');
    expect(call.request.params.get('sortBy')).toBe('status');
    expect(call.request.params.get('direction')).toBe('desc');
    expect(call.request.params.get('keyword')).toBe('test');
    call.flush({ content: [], totalElements: 0 });
  });
  it('always sends sortBy and omits blank search to receive a Page instead of an array', () => {
    service.list({ page: 0, size: 10, sortBy: 'payPeriod', direction: 'asc', keyword: ' ' }).subscribe();
    const call = http.expectOne(r => r.url === url);
    expect(call.request.params.get('sortBy')).toBe('payPeriod');
    expect(call.request.params.has('keyword')).toBeFalse(); call.flush({ content: [] });
  });
  it('reads a detail through Gateway', () => {
    service.get(4).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('GET'); call.flush({ id: 4, ...request });
  });
  it('creates using only actual request fields', () => {
    service.create(request).subscribe(); const call = http.expectOne(url);
    expect(call.request.method).toBe('POST'); expect(call.request.body).toEqual(request); call.flush({ id: 4, ...request });
  });
  it('updates the requested payroll', () => {
    service.update(4, request).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('PUT'); expect(call.request.body).toEqual(request); call.flush({ id: 4, ...request });
  });
  it('deletes the requested payroll', () => {
    service.delete(4).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('DELETE'); call.flush(null);
  });
  it('publishes or unpublishes a payroll via the dedicated admin endpoint', () => {
    service.setPublished(4, true).subscribe(); const call = http.expectOne(url + '/4/publication');
    expect(call.request.method).toBe('PATCH'); expect(call.request.body).toEqual({ published: true });
    call.flush({ id: 4, ...request, published: true });
  });
  const summary={paidAmount:'40.00',remainingAmount:'60.00',state:'PARTIAL',entries:[],correctionReason:null,correctedBy:null,correctedAt:null};
  it('reads and validates the ledger contract',()=>{service.settlements(7).subscribe(value=>expect(value.paidAmount).toBe('40.00'));http.expectOne(url+'/7/settlements').flush(summary);});
  it('rejects a malformed ledger rather than showing zero',()=>{service.settlements(7).subscribe({next:()=>fail('invalid response accepted'),error:e=>expect(e.status).toBe(502)});http.expectOne(url+'/7/settlements').flush({...summary,paidAmount:40});});
  it('posts payment and reversal payloads with stable request identities',()=>{const payment={requestId:'request',amount:10,paidAt:'2026-11-02',method:null,transactionReference:null};service.pay(7,payment).subscribe();let call=http.expectOne(url+'/7/settlements');expect(call.request.method).toBe('POST');expect(call.request.body).toEqual(payment);call.flush(summary);service.reverse(7,'reference','second-request','Erreur').subscribe();call=http.expectOne(url+'/7/settlements/reference/reversal');expect(call.request.body).toEqual({requestId:'second-request',reason:'Erreur'});call.flush(summary);});
  it('requests a correction draft with a reason',()=>{service.correct(7,'Prime incorrecte').subscribe();const call=http.expectOne(url+'/7/corrections');expect(call.request.body).toEqual({reason:'Prime incorrecte'});call.flush({id:8});});
  it('downloads the explicit period as a CSV blob',()=>{service.exportTechnical('2026-11').subscribe();const call=http.expectOne(r=>r.url===url+'/export/technical.csv');expect(call.request.params.get('period')).toBe('2026-11');expect(call.request.responseType).toBe('blob');call.flush(new Blob(['TECHNICAL_NON_REGULATORY']));});
});
