import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { of,Subject,throwError } from 'rxjs';
import { CustomerAssociationsComponent } from './customer-associations.component';
import { ClientService } from './client.service';
import { AuthService } from '../../core/services/auth.service';
describe('customer administrative association',()=>{
 let service:jasmine.SpyObj<ClientService>;
 beforeEach(()=>{service=jasmine.createSpyObj('ClientService',['users','customers','association','associate']);
  service.users.and.returnValue(of([{id:'42',username:'client',roles:['CLIENT'],enabled:true,accountNonLocked:true}]));
  service.customers.and.returnValue(of([{id:'101',name:'Customer A'}]));service.association.and.returnValue(throwError(()=>({status:404})));
  TestBed.configureTestingModule({imports:[CustomerAssociationsComponent],providers:[provideZonelessChangeDetection(),
   {provide:ClientService,useValue:service},{provide:AuthService,useValue:{sessionGeneration:0,getCurrentUser:()=>({}),hasAnyRole:()=>true}}]});
 });
 function create(){const f=TestBed.createComponent(CustomerAssociationsComponent);f.detectChanges();f.componentInstance['uid']='42';f.componentInstance['cid']='101';return f;}
 it('creates an explicit association once and prevents repeated submissions',()=>{
  const p=new Subject<{userId:string;customerId:string}>();service.associate.and.returnValue(p);
  const f=create();f.componentInstance['lookup']();f.componentInstance['create']();f.componentInstance['create']();expect(service.associate).toHaveBeenCalledOnceWith('42','101');
  p.next({userId:'42',customerId:'101'});p.complete();f.componentInstance['create']();expect(service.associate).toHaveBeenCalledTimes(1);
 });
 it('does not permit reassignment of an existing association',()=>{
  service.association.and.returnValue(of({userId:'42',customerId:'202'}));const f=create();f.componentInstance['lookup']();f.componentInstance['create']();
  expect(service.associate).not.toHaveBeenCalled();expect(f.componentInstance['message']()).toContain('202');
 });
 it('displays conflicts and blocks further submissions',()=>{
  service.associate.and.returnValue(throwError(()=>({status:409})));const f=create();f.componentInstance['lookup']();f.componentInstance['create']();
  expect(f.componentInstance['error']()).toContain('conflit');expect(f.componentInstance['linked']()).toBeTrue();
 });
});
