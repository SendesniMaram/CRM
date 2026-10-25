import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection,signal } from '@angular/core';
import { Subject,of,throwError } from 'rxjs';
import { ClientWorkspaceComponent } from './client-workspace.component';
import { ClientService,ClientProfile } from './client.service';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
describe('ClientWorkspaceComponent',()=>{
 const user=signal('A');let generation=0;let allowed=true;
 let service:jasmine.SpyObj<ClientService>;
 beforeEach(()=>{generation=0;allowed=true;user.set('A');
  service=jasmine.createSpyObj('ClientService',['profile','invoices','detail','pdf']);
  service.profile.and.returnValue(of({customerId:'101',name:'Customer A',email:null,phone:null,company:'Company A'}));
  service.invoices.and.returnValue(of({content:[{id:'12',invoiceNumber:'INV-A',issueDate:null,dueDate:null,totalAmount:'123.40',currency:'EUR',status:'PENDING'}],totalElements:1,totalPages:1,number:0}));
  service.detail.and.returnValue(of({id:'12',invoiceNumber:'INV-A',issueDate:null,dueDate:null,totalAmount:'123.40',currency:'EUR',status:'PENDING'}));
  TestBed.configureTestingModule({imports:[ClientWorkspaceComponent],providers:[provideZonelessChangeDetection(),
   {provide:ClientService,useValue:service},{provide:AuthService,useValue:{
    getCurrentUser:()=>({username:user()}),get sessionGeneration(){return generation;},hasAnyRole:(r:CrmRole[])=>{user();return allowed&&r.includes(CrmRole.CLIENT);}
   }}]});
 });
 function create(){const f=TestBed.createComponent(ClientWorkspaceComponent);f.detectChanges();return f;}
 it('renders actual profile, count, invoice status and amount',()=>{
  const f=create();expect(f.nativeElement.textContent).toContain('Customer A');expect(f.nativeElement.textContent).toContain('123.40');
  expect(f.nativeElement.textContent).toContain('INV-A');expect(f.nativeElement.textContent).toContain('Mon profil');
 });
 it('clears all private data on session change and cancels previous requests',()=>{
  const old=new Subject<ClientProfile>();service.profile.and.returnValue(old);const f=create();expect(old.observed).toBeTrue();
  generation++;allowed=false;user.set('B');f.detectChanges();expect(old.observed).toBeFalse();expect(f.componentInstance['profile']()).toBeNull();expect(f.componentInstance['page']()).toBeNull();
 });
 it('reloads when a new account has the same CLIENT role',()=>{
  const f=create();generation++;user.set('B');service.profile.and.returnValue(of({customerId:'202',name:'Customer B',email:null,phone:null,company:null}));
  f.detectChanges();expect(f.nativeElement.textContent).toContain('Customer B');expect(f.nativeElement.textContent).not.toContain('Customer A');
 });
 for(const status of [401,403,404,502,503]){
  it('displays HTTP '+status+' and clears stale data',()=>{
   service.profile.and.returnValue(throwError(()=>({status})));const f=create();
   expect(f.nativeElement.querySelector('[role=alert]')).toBeTruthy();expect(f.componentInstance['profile']()).toBeNull();expect(f.componentInstance['page']()).toBeNull();
  });
 }
 it('shows loading and empty invoices separately',()=>{
  const pending=new Subject<ClientProfile>();service.profile.and.returnValue(pending);service.invoices.and.returnValue(of({content:[],totalElements:0,totalPages:0,number:0}));
  const f=create();expect(f.nativeElement.textContent).toContain('Chargement');
  pending.next({customerId:'101',name:'A',email:null,phone:null,company:null});pending.complete();f.detectChanges();expect(f.nativeElement.textContent).toContain('Aucune facture');
 });
 it('uses the personal detail API and blocks double detail submissions',()=>{
  const pending=new Subject<any>();service.detail.and.returnValue(pending);const f=create();
  f.componentInstance['open']('12');f.componentInstance['open']('12');expect(service.detail).toHaveBeenCalledTimes(1);f.destroy();expect(pending.observed).toBeFalse();
 });
 it('does not fetch anything without CLIENT role',()=>{allowed=false;create();expect(service.profile).not.toHaveBeenCalled();expect(service.invoices).not.toHaveBeenCalled();});
 it('labels filters as current-page filters and preserves the server total',()=>{
  service.invoices.and.returnValue(of({content:[{id:'9223372036854775807',invoiceNumber:'INV-LONG',issueDate:null,dueDate:null,totalAmount:'10.00',currency:'EUR',status:'PAID'},
   {id:'13',invoiceNumber:'OTHER',issueDate:null,dueDate:null,totalAmount:'20.00',currency:'TND',status:'PENDING'}],totalElements:42,totalPages:5,number:0}));
  const f=create(),page=f.componentInstance as any;page.query.set('inv');page.statusFilter.set('PAID');f.detectChanges();
  expect(page.visibleInvoices().map((r:any)=>r.id)).toEqual(['9223372036854775807']);expect(page.page().totalElements).toBe(42);
  expect(f.nativeElement.textContent).toContain('uniquement sur les 2 factures');
  page.open(page.visibleInvoices()[0].id);expect(service.detail).toHaveBeenCalledWith('9223372036854775807');
 });
 it('paginates without refetching an unchanged profile and resets page-local filters',()=>{
  const f=create(),page=f.componentInstance as any;page.query.set('old');page.statusFilter.set('PENDING');
  service.invoices.and.returnValue(of({content:[],totalElements:11,totalPages:2,number:1}));page.load(1);
  expect(service.profile).toHaveBeenCalledTimes(1);expect(service.invoices).toHaveBeenCalledWith(1);
  expect(page.query()).toBe('');expect(page.statusFilter()).toBe('');
  page.load(-1);expect(service.invoices).toHaveBeenCalledTimes(2);
 });
 it('does not change invoice pages while a detail is pending',()=>{
  const f=create(),page=f.componentInstance as any,pending=new Subject<any>();service.detail.and.returnValue(pending);
  page.open('12');page.load(1);expect(service.invoices).toHaveBeenCalledTimes(1);
 });
 it('ends loading with a communication error when the personal API never responds',()=>{
  jasmine.clock().install();try{
   service.profile.and.returnValue(new Subject<ClientProfile>());const f=create();
   expect(f.componentInstance['loading']()).toBeTrue();jasmine.clock().tick(10001);f.detectChanges();
   expect(f.componentInstance['loading']()).toBeFalse();expect(f.nativeElement.textContent).toContain('Erreur de communication');
  }finally{jasmine.clock().uninstall();}
 });
});
