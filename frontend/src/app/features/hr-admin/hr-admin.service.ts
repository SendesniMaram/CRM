import { HttpClient,HttpErrorResponse } from '@angular/common/http';
import { Injectable,inject } from '@angular/core';
import { map } from 'rxjs';
import { canonicalId,parseDirectory } from '../users/association-identifiers';
import { HrModule,HrRow,HrPage,HR_MODULES } from './hr-admin.models';

@Injectable({providedIn:'root'})
export class HrAdminService {
  private readonly http=inject(HttpClient);
  private url(module:HrModule) {
    if(!Object.prototype.hasOwnProperty.call(HR_MODULES,module))throw new Error('Invalid HR module');
    return 'http://localhost:8080/hr/api/'+module;
  }
  list(module:HrModule,page=0,keyword='') {
    if(!Number.isSafeInteger(page)||page<0)throw new Error('Invalid page');
    // sortBy forces the existing controller's paged response, including the first page.
    return this.http.get(this.url(module),{params:{page,size:10,sortBy:'id',direction:'desc',...(keyword?{keyword}: {})},responseType:'text'}).pipe(map(text=>{
      const result=parseDirectory(text) as HrPage;
      if(!result||!Array.isArray(result.content)||result.content.some(row=>!this.validRow(module,row))
        ||new Set(result.content.map(row=>row.id)).size!==result.content.length
        ||!Number.isSafeInteger(result.totalElements)||result.totalElements<0
        ||!Number.isSafeInteger(result.totalPages)||result.totalPages<0||result.number!==page)throw this.invalid();
      return result;
    }));
  }
  read(module:HrModule,id:string) {
    this.id(id);
    return this.http.get(this.url(module)+'/'+id,{responseType:'text'}).pipe(map(text=>this.row(module,text,id)));
  }
  save(module:HrModule,id:string|null,request:Record<string,unknown>) {
    if(id)this.id(id);
    // Never submit response-only fields (certificate, timestamps or arbitrary form fields).
    const body:Record<string,unknown>={};
    for(const field of HR_MODULES[module].fields)body[field.key]=request[field.key]??null;
    const result=id?this.http.put(this.url(module)+'/'+id,body,{responseType:'text'})
      :this.http.post(this.url(module),body,{responseType:'text'});
    return result.pipe(map(text=>this.row(module,text,id)));
  }
  delete(module:HrModule,id:string) {this.id(id);return this.http.delete<void>(this.url(module)+'/'+id);}
  private row(module:HrModule,text:string,expected:string|null) {
    const row=parseDirectory(text) as HrRow;
    if(!this.validRow(module,row)||(expected!==null&&row.id!==expected))throw this.invalid();
    return row;
  }
  private validRow(module:HrModule,row:HrRow):boolean {
    if(!row||!canonicalId(row.id)||typeof row.employeeId!=='string'||!row.employeeId.trim()||row.employeeId.length>50)return false;
    for(const field of HR_MODULES[module].fields) {
      const value=row[field.key];
      if(value===null||value===undefined){if(field.required)return false;continue;}
      if(field.type==='number'){if(typeof value!=='number'||!Number.isFinite(value)||value<0)return false;}
      else if(typeof value!=='string'||(field.maxLength&&value.length>field.maxLength)
        ||(field.options&&!field.options.includes(value)))return false;
    }
    return true;
  }
  private invalid(){return new HttpErrorResponse({status:502});}
  private id(id:string){if(!canonicalId(id))throw new Error('Invalid identifier');}
}
