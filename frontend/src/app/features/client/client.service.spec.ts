import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { ClientService,clientError } from './client.service';
describe('ClientService personal contracts',()=>{
 let service:ClientService;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(ClientService);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('never sends a target identity for profile or invoice list',()=>{
  service.profile().subscribe(r=>expect(r.customerId).toBe('9223372036854775807'));
  const p=http.expectOne('http://localhost:8080/customer/api/customers/me');expect(p.request.params.keys()).toEqual([]);
  p.flush({customerId:'9223372036854775807',name:'A',email:null,phone:null,company:null});
  service.invoices().subscribe(r=>expect(r.totalElements).toBe(0));
  const q=http.expectOne(r=>r.url.endsWith('/invoices/me'));expect(q.request.params.keys()).toEqual(['page','size']);
  q.flush({content:[],totalElements:0,totalPages:0,number:0});
 });
 it('keeps long association identifiers exact',()=>{
  service.customers().subscribe(rows=>expect(rows[0].id).toBe('9223372036854775807'));
  http.expectOne('http://localhost:8080/customer/api/customers').flush('[{"id":9223372036854775807,"name":"Long"}]');
  service.associate('9223372036854775807','9007199254740993').subscribe();
  const r=http.expectOne('http://localhost:8080/customer/api/customers/associations');
  expect(r.request.body).toEqual({userId:'9223372036854775807',customerId:'9007199254740993'});r.flush(r.request.body);
 });
 it('rejects malformed profiles and incorrect detail identities',()=>{
  service.profile().subscribe({error:e=>expect(e.status).toBe(502)});
  http.expectOne('http://localhost:8080/customer/api/customers/me').flush({customerId:'01',name:'A'});
  service.detail('1').subscribe({error:e=>expect(e.status).toBe(502)});
  http.expectOne('http://localhost:8080/invoice/api/invoices/me/1').flush({id:'2',invoiceNumber:'B',totalAmount:'1.00',currency:'EUR',status:'PAID'});
 });
 it('rejects invalid requested IDs before HTTP',()=>{
  expect(()=>service.detail('01')).toThrow();expect(()=>service.pdf('90071992547409930x')).toThrow();http.expectNone(()=>true);
 });
 it('downloads only its personal PDF endpoint',()=>{
  service.pdf('9007199254740993').subscribe(blob=>expect(blob.size).toBe(4));
  http.expectOne('http://localhost:8080/invoice/api/invoices/me/9007199254740993/pdf').flush(new Blob(['%PDF'],{type:'application/pdf'}),{headers:{'Content-Type':'application/pdf'}});
 });
 for(const status of [400,401,403,404,409,502,503,0]){
  it('handles HTTP '+status+' without exposing diagnostic content',()=>{expect(clientError({status})).not.toContain('undefined');expect(clientError({status}).length).toBeGreaterThan(5);});
 }
});
