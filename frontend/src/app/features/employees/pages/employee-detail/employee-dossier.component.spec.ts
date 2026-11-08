import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeDossierComponent } from './employee-dossier.component';
describe('document sharing administration',()=>{
 const base='http://localhost:8080/hr/api';
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({imports:[EmployeeDossierComponent],providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 function create(visible=false){const f=TestBed.createComponent(EmployeeDossierComponent);f.componentRef.setInput('employeeId',103);f.detectChanges();http.expectOne(base+'/employees/103/documents').flush([{id:7,name:'Attestation',type:'CERTIFICATE',size:4,createdAt:'2026-11-01',employeeVisible:visible}]);http.expectOne(base+'/employees/103/career-events').flush([]);f.detectChanges();return f;}
 it('offers explicit sharing for private documents',()=>{const f=create();expect(f.nativeElement.textContent).toContain('Partager avec le salarié');f.componentInstance['share']({id:7,employeeVisible:false} as any);const r=http.expectOne(base+'/documents/7/visibility');expect(r.request.method).toBe('PATCH');expect(r.request.body).toEqual({employeeVisible:true});r.flush(null,{status:204,statusText:'No Content'});http.expectOne(base+'/employees/103/documents').flush([]);http.expectOne(base+'/employees/103/career-events').flush([]);});
 it('offers withdrawal for a shared document',()=>{const f=create(true);expect(f.nativeElement.textContent).toContain('Retirer le partage salarié');f.componentInstance['share']({id:7,employeeVisible:true} as any);const r=http.expectOne(base+'/documents/7/visibility');expect(r.request.body).toEqual({employeeVisible:false});r.flush('denied',{status:403,statusText:'Forbidden'});f.detectChanges();expect(f.nativeElement.querySelector('[role=alert]')).toBeTruthy();});
});
