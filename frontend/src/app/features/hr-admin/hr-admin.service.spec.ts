import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { HrAdminService } from './hr-admin.service';
import { HrModule } from './hr-admin.models';

const fixtures:Record<HrModule,Record<string,unknown>>={
  trainings:{id:7,employeeId:'101',title:'Formation sécurité',provider:'Organisme',startDate:'2099-01-01',endDate:'2099-01-02',cost:0,status:'PLANNED',certificateObtained:false},
  attendances:{id:7,employeeId:'101',date:'2026-10-23',checkIn:'09:00:00',checkOut:'17:00:00',status:'PRESENT',notes:null},
  'hr-records':{id:7,employeeId:'101',contractType:'CDI',hireDate:'2026-10-23T09:00:00',jobTitle:'Consultant',salaryGrade:null,manager:null,workLocation:null,employmentStatus:'ACTIVE'},
  contracts:{id:7,employeeId:'101',contractType:'CDI',startDate:'2026-10-23',endDate:null,status:'ACTIVE',parentContractId:null,notes:null},
  evaluations:{id:7,employeeId:'101',periodStart:'2026-01-01',periodEnd:'2026-12-31',criteria:null,comments:null,status:'DRAFT'},
  'job-offers':{id:7,title:'Développeur',description:null,status:'OPEN'},
  applications:{id:7,offerId:7,candidateName:'Candidate',candidateEmail:'candidate@example.test',notes:null,status:'RECEIVED'}
};
describe('HR administrative HTTP contracts',()=>{
  let service:HrAdminService;let http:HttpTestingController;
  beforeEach(()=>{
    TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting()]});
    service=TestBed.inject(HrAdminService);http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>http.verify());
  for(const module of Object.keys(fixtures) as HrModule[]){
    it(module+': requests an explicit paged response and preserves employee references',()=>{
      service.list(module,0,'sécurité').subscribe(page=>{
        expect(page.content[0].id).toBe('7');
        if('employeeId' in fixtures[module])expect(page.content[0].employeeId).toBe('101');
      });
      const r=http.expectOne(r=>r.url==='http://localhost:8080/hr/api/'+module);
      expect(r.request.params.get('sortBy')).toBe('id');expect(r.request.params.get('size')).toBe('10');
      expect(r.request.params.get('keyword')).toBe('sécurité');
      r.flush(JSON.stringify({content:[fixtures[module]],totalElements:1,totalPages:1,number:0}));
    });
    it(module+': supports creation, detail, update and deletion without sending server-only fields',()=>{
      service.save(module,null,{...fixtures[module],arbitraryRole:'SUPER_ADMIN'}).subscribe(row=>expect(row.id).toBe('7'));
      const post=http.expectOne('http://localhost:8080/hr/api/'+module);expect(post.request.method).toBe('POST');
      for(const key of ['id','createdAt','arbitraryRole'])expect(post.request.body[key]).toBeUndefined();
      if(module==='trainings')expect(post.request.body['certificateObtained']).toBe(false);
      else expect(post.request.body['certificateObtained']).toBeUndefined();
      post.flush(JSON.stringify(fixtures[module]));
      service.read(module,'7').subscribe(row=>expect(row.id).toBe('7'));
      http.expectOne('http://localhost:8080/hr/api/'+module+'/7').flush(JSON.stringify(fixtures[module]));
      service.save(module,'7',fixtures[module]).subscribe();
      const put=http.expectOne('http://localhost:8080/hr/api/'+module+'/7');expect(put.request.method).toBe('PUT');put.flush(JSON.stringify(fixtures[module]));
      service.delete(module,'7').subscribe();
      const del=http.expectOne('http://localhost:8080/hr/api/'+module+'/7');expect(del.request.method).toBe('DELETE');del.flush(null,{status:204,statusText:'No Content'});
    });
    it(module+': refuses malformed and inconsistent remote responses',()=>{
      service.read(module,'7').subscribe({error:e=>expect(e.status).toBe(502)});
      http.expectOne('http://localhost:8080/hr/api/'+module+'/7').flush(JSON.stringify({...fixtures[module],id:8}));
      service.list(module).subscribe({error:e=>expect(e.status).toBe(502)});
      http.expectOne(r=>r.url.endsWith('/'+module)).flush(JSON.stringify({content:[],totalElements:-1,totalPages:0,number:0}));
    });
  }
  it('keeps long numeric ids exact before JSON conversion',()=>{
    service.read('hr-records','9223372036854775807').subscribe(r=>expect(r.id).toBe('9223372036854775807'));
    http.expectOne('http://localhost:8080/hr/api/hr-records/9223372036854775807').flush('{"id":9223372036854775807,"employeeId":"101"}');
  });
  it('rejects malformed paths and negative pages before sending HTTP',()=>{
    expect(()=>service.read('trainings','01')).toThrow();expect(()=>service.delete('trainings','../users')).toThrow();
    expect(()=>service.list('trainings',-1)).toThrow();http.expectNone(()=>true);
  });
  for(const status of [400,401,403,404,409,502,503]){
    it('preserves HTTP '+status+' for UI handling',()=>{
      service.list('trainings').subscribe({error:e=>expect(e.status).toBe(status)});
      http.expectOne(r=>r.url.endsWith('/trainings')).flush({message:'synthetic error'},{status,statusText:'Error'});
    });
  }
});
