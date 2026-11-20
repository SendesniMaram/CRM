import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MyDocumentsService, PersonalDocument } from './my-documents.service';
describe('MyDocumentsService',()=>{
 let service:MyDocumentsService,http:HttpTestingController;
 const base='http://localhost:8080/hr/api/documents/me';
 const document:PersonalDocument={id:'9223372036854775807',name:'attestation.pdf',type:'CERTIFICATE',mediaType:'application/pdf',size:4,createdAt:'2026-11-01T10:00:00'};
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(MyDocumentsService);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('lists without supplying an owner and preserves long identifiers',()=>{service.list().subscribe(rows=>expect(rows[0].id).toBe(document.id));const r=http.expectOne(base);expect(r.request.params.keys()).toEqual([]);r.flush([document]);});
 it('accepts an empty personal list',()=>{service.list().subscribe(rows=>expect(rows).toEqual([]));http.expectOne(base).flush([]);});
 for(const invalid of [{id:'01'},{mediaType:'text/html'},{size:10485761},{name:null}]){
  it('rejects malformed document '+JSON.stringify(invalid),()=>{service.list().subscribe({error:e=>expect(e.status).toBe(502)});http.expectOne(base).flush([{...document,...invalid}]);});
 }
 it('downloads through the personal endpoint only',()=>{service.download(document).subscribe(blob=>expect(blob.size).toBe(4));const r=http.expectOne(base+'/'+document.id+'/content');expect(r.request.params.keys()).toEqual([]);r.flush(new Blob(['%PDF']),{headers:{'Content-Type':'application/pdf'}});});
 it('rejects a mismatched download media type',()=>{service.download(document).subscribe({error:e=>expect(e.status).toBe(502)});http.expectOne(base+'/'+document.id+'/content').flush(new Blob(['html']),{headers:{'Content-Type':'text/html'}});});
 it('rejects invalid IDs before HTTP',()=>{expect(()=>service.download({...document,id:'../1'})).toThrow();http.expectNone(()=>true);});
});
