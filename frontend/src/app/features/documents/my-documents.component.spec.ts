import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { MyDocumentsComponent } from './my-documents.component';
import { MyDocumentsService, PersonalDocument } from './my-documents.service';
describe('MyDocumentsComponent',()=>{
 let service:jasmine.SpyObj<MyDocumentsService>,generation:number,allowed:boolean;
 const user=signal('A');
 beforeEach(()=>{generation=0;allowed=true;user.set('A');service=jasmine.createSpyObj('MyDocumentsService',['list','download']);service.list.and.returnValue(of([]));
  TestBed.configureTestingModule({imports:[MyDocumentsComponent],providers:[provideZonelessChangeDetection(),{provide:MyDocumentsService,useValue:service},{provide:AuthService,useValue:{getCurrentUser:()=>user(),get sessionGeneration(){return generation;},hasAnyRole:(roles:CrmRole[])=>{user();return allowed&&roles.includes(CrmRole.EMPLOYEE);}}}]});
 });
 function create(){const f=TestBed.createComponent(MyDocumentsComponent);f.detectChanges();return f;}
 it('shows an empty shared list',()=>{expect(create().nativeElement.textContent).toContain('Aucun document partagé');});
 it('renders the real shared document',()=>{service.list.and.returnValue(of([{id:'7',name:'Attestation A',type:'CERTIFICATE',mediaType:'application/pdf',size:4,createdAt:'2026-11-01T10:00:00'}]));expect(create().nativeElement.textContent).toContain('Attestation A');});
 for(const status of [401,403,404,502,503])it('displays a fixed error for '+status,()=>{service.list.and.returnValue(throwError(()=>({status,message:'private diagnostic'})));const f=create();expect(f.nativeElement.querySelector('[role=alert]')).toBeTruthy();expect(f.nativeElement.textContent).not.toContain('private diagnostic');expect(f.componentInstance['rows']()).toBeNull();});
 it('cancels pending calls and clears data after a session change',()=>{const pending=new Subject<PersonalDocument[]>();service.list.and.returnValue(pending);const f=create();expect(pending.observed).toBeTrue();generation++;allowed=false;user.set('B');f.detectChanges();expect(pending.observed).toBeFalse();expect(f.componentInstance['rows']()).toBeNull();});
 it('does not call HR for another role',()=>{allowed=false;create();expect(service.list).not.toHaveBeenCalled();});
 it('ends a pending load with a timeout message',()=>{jasmine.clock().install();try{service.list.and.returnValue(new Subject<PersonalDocument[]>());const f=create();jasmine.clock().tick(10001);f.detectChanges();expect(f.componentInstance['loading']()).toBeFalse();expect(f.nativeElement.querySelector('[role=alert]')).toBeTruthy();}finally{jasmine.clock().uninstall();}});
});
