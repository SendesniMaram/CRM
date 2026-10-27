import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection,signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject,of,throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { HrAdminService } from './hr-admin.service';
import { HrAdminPageComponent } from './hr-admin-page.component';
import { HR_MODULES,HrModule,HrRow } from './hr-admin.models';
const fixtures:Record<HrModule,HrRow>={
  trainings:{id:'7',employeeId:'101',title:'Formation sécurité',provider:'Organisme',startDate:'2099-01-01',endDate:'2099-01-02',cost:0,status:'PLANNED',certificateObtained:false},
  attendances:{id:'7',employeeId:'101',date:'2026-10-23',checkIn:'09:00:00',checkOut:'17:00:00',status:'PRESENT',notes:null},
  'hr-records':{id:'7',employeeId:'101',contractType:'CDI',hireDate:'2026-10-23T09:00:00',jobTitle:'Consultant',salaryGrade:null,manager:null,workLocation:null,employmentStatus:'ACTIVE'},
  contracts:{id:'7',employeeId:'101',contractType:'CDI',startDate:'2026-10-23',endDate:null,status:'ACTIVE',parentContractId:null,notes:null},
  evaluations:{id:'7',employeeId:'101',periodStart:'2026-01-01',periodEnd:'2026-12-31',criteria:null,comments:null,status:'DRAFT'},
  'job-offers':{id:'7',title:'Développeur',description:null,status:'OPEN'},
  applications:{id:'7',offerId:7,candidateName:'Candidate',candidateEmail:'candidate@example.test',notes:null,status:'RECEIVED'}
};
describe('HR administrative screens',()=>{
  const identity=signal('admin');let generation=0;let role=CrmRole.ADMIN;
  let service:jasmine.SpyObj<HrAdminService>;let dialog:{open:jasmine.Spy};let module:HrModule;
  beforeEach(()=>{
    identity.set('admin');generation=0;role=CrmRole.ADMIN;module='trainings';
    service=jasmine.createSpyObj('HrAdminService',['list','read','save','delete']);
    service.list.and.callFake(m=>of({content:[fixtures[m]],totalElements:1,totalPages:1,number:0}));
    service.read.and.callFake(m=>of(fixtures[m]));service.save.and.callFake(m=>of(fixtures[m]));service.delete.and.returnValue(of(void 0));
    dialog={open:jasmine.createSpy('open').and.returnValue({afterClosed:()=>of(true)})};
  });
  function create(m:HrModule='trainings'){
    module=m;
    TestBed.configureTestingModule({imports:[HrAdminPageComponent],providers:[provideZonelessChangeDetection(),
      {provide:HrAdminService,useValue:service},{provide:MatDialog,useValue:dialog},
      {provide:ActivatedRoute,useValue:{snapshot:{data:{hrModule:module}}}},
      {provide:AuthService,useValue:{
        getCurrentUser:()=>({username:identity()}),get sessionGeneration(){return generation;},
        hasAnyRole:(roles:CrmRole[])=>{identity();return roles.includes(role);}
      }}]});
    const f=TestBed.createComponent(HrAdminPageComponent);f.detectChanges();return f;
  }
  function fill(f:ReturnType<typeof create>,m:HrModule){
    for(const field of HR_MODULES[m].fields)f.componentInstance['form'].controls[field.key].setValue(fixtures[m][field.key]??'');
  }
  for(const m of Object.keys(HR_MODULES) as HrModule[]){
    for(const r of [CrmRole.ADMIN,CrmRole.SUPER_ADMIN]){
      it(m+': offers CRUD to '+r,()=>{
        role=r;const f=create(m);expect(f.nativeElement.textContent).toContain(HR_MODULES[m].title);
        const expected=Object.fromEntries(HR_MODULES[m].fields.map(field=>[field.key,fixtures[m][field.key]??null]));
        f.componentInstance['create']();fill(f,m);f.componentInstance['save']();
        expect(service.save).toHaveBeenCalledWith(m,null,jasmine.objectContaining(expected));
        f.componentInstance['open'](fixtures[m],true);expect(service.read).toHaveBeenCalledWith(m,'7');
        f.componentInstance['save']();expect(service.save).toHaveBeenCalledWith(m,'7',jasmine.objectContaining(expected));
        f.componentInstance['remove'](fixtures[m]);expect(dialog.open).toHaveBeenCalled();expect(service.delete).toHaveBeenCalledWith(m,'7');
      });
    }
    it(m+': shows details separately and does not submit when viewing',()=>{
      const f=create(m);f.componentInstance['open'](fixtures[m]);f.detectChanges();
      expect(f.nativeElement.textContent).toContain('Détail');f.componentInstance['save']();expect(service.save).not.toHaveBeenCalled();
    });
    it(m+': validates required fields and supports cancellation',()=>{
      const f=create(m);f.componentInstance['create']();f.componentInstance['save']();expect(service.save).not.toHaveBeenCalled();
      expect(f.componentInstance['formError']()).toContain('Vérifiez');f.componentInstance['cancel']();expect(f.componentInstance['editing']()).toBeFalse();
    });
  }
  it('searches and paginates using backend parameters',()=>{
    const f=create();f.componentInstance['search'].setValue('sécurité');f.componentInstance['find']();
    expect(service.list).toHaveBeenCalledWith('trainings',0,'sécurité');
    f.componentInstance['paginate']({pageIndex:1,pageSize:10,length:20,previousPageIndex:0});
    expect(service.list).toHaveBeenCalledWith('trainings',1,'sécurité');
  });
  it('cancels deletion when confirmation is declined',()=>{
    dialog.open.and.returnValue({afterClosed:()=>of(false)});const f=create();f.componentInstance['remove'](fixtures.trainings);
    expect(service.delete).not.toHaveBeenCalled();expect(f.componentInstance['busy']()).toBeFalse();
  });
  it('prevents duplicate save submissions',()=>{
    const pending=new Subject<HrRow>();service.save.and.returnValue(pending);const f=create();
    f.componentInstance['create']();fill(f,'trainings');f.componentInstance['save']();f.componentInstance['save']();
    expect(service.save).toHaveBeenCalledTimes(1);f.destroy();expect(pending.observed).toBeFalse();
  });
  it('prevents duplicate delete submissions while confirmation is pending',()=>{
    const pending=new Subject<boolean>();dialog.open.and.returnValue({afterClosed:()=>pending});const f=create();
    f.componentInstance['remove'](fixtures.trainings);f.componentInstance['remove'](fixtures.trainings);
    expect(dialog.open).toHaveBeenCalledTimes(1);f.destroy();expect(pending.observed).toBeFalse();
  });
  it('refuses creation as completed and invalid date ranges without HTTP',()=>{
    const f=create();f.componentInstance['create']();fill(f,'trainings');f.componentInstance['form'].patchValue({status:'COMPLETED'});f.componentInstance['save']();
    expect(service.save).not.toHaveBeenCalled();expect(f.componentInstance['formError']()).toContain('avant');
    f.componentInstance['form'].patchValue({status:'PLANNED',endDate:'2098-01-01'});f.componentInstance['save']();
    expect(service.save).not.toHaveBeenCalled();expect(f.componentInstance['formError']()).toContain('dates');
  });
  it('keeps completed trainings read-only even when the list snapshot was stale',()=>{
    service.read.and.returnValue(of({...fixtures.trainings,status:'COMPLETED'}));const f=create();f.componentInstance['open'](fixtures.trainings,true);
    expect(f.componentInstance['editing']()).toBeFalse();expect(f.componentInstance['viewing']()).toBeTrue();
  });
  for(const r of [CrmRole.CLIENT,CrmRole.EMPLOYEE]){
    it('does not fetch or mutate with '+r,()=>{
      role=r;const f=create();f.componentInstance['create']();f.componentInstance['open'](fixtures.trainings);f.componentInstance['remove'](fixtures.trainings);
      expect(service.list).not.toHaveBeenCalled();expect(service.read).not.toHaveBeenCalled();expect(service.delete).not.toHaveBeenCalled();
    });
  }
  it('clears administrative values and cancels pending responses after a session change',()=>{
    const pending=new Subject<HrRow>();service.read.and.returnValue(pending);const f=create();f.componentInstance['open'](fixtures.trainings);
    generation++;role=CrmRole.CLIENT;identity.set('client');f.detectChanges();
    expect(pending.observed).toBeFalse();expect(f.componentInstance['rows']()).toEqual([]);expect(f.componentInstance['selected']()).toBeNull();
  });
  it('does not delete after confirmation from an older session',()=>{
    const pending=new Subject<boolean>();dialog.open.and.returnValue({afterClosed:()=>pending});const f=create();f.componentInstance['remove'](fixtures.trainings);
    generation++;identity.set('other-admin');pending.next(true);expect(service.delete).not.toHaveBeenCalled();
  });
  it('reloads for a new admin account even if the username is reused',()=>{
    const f=create();generation++;identity.set('intermediate');f.detectChanges();identity.set('admin');f.detectChanges();
    expect(service.list).toHaveBeenCalledTimes(2);
  });
  for(const status of [400,401,403,404,409,502,503,0]){
    it('handles save errors '+status+' without losing form values',()=>{
      service.save.and.returnValue(throwError(()=>({status})));const f=create();f.componentInstance['create']();fill(f,'trainings');f.componentInstance['save']();
      expect(f.componentInstance['formError']().length).toBeGreaterThan(0);expect(f.componentInstance['busy']()).toBeFalse();
      expect(f.componentInstance['form'].controls['employeeId'].value).toBe('101');
    });
  }
  it('distinguishes empty lists, loading and backend failures',()=>{
    const pending=new Subject<any>();service.list.and.returnValue(pending);const f=create();expect(f.componentInstance['loading']()).toBeTrue();
    pending.next({content:[],totalElements:0,totalPages:0,number:0});f.detectChanges();expect(f.nativeElement.textContent).toContain('Aucun enregistrement');
    service.list.and.returnValue(throwError(()=>({status:503})));f.componentInstance['load']();f.detectChanges();
    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('indisponible');
  });
});
