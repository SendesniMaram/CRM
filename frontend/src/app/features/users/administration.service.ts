import { Injectable,inject } from '@angular/core';
import { HttpClient,HttpErrorResponse } from '@angular/common/http';
import { map } from 'rxjs';
import { parseDirectory,canonicalId } from './association-identifiers';
export interface AdminPage<T>{content:T[];number:number;totalPages:number;totalElements:number}
export interface DisplaySettings{companyName:string;supportEmail:string;welcomeMessage:string;version:number}
export interface AuditEvent{id:string;occurredAt:string;actorId:string|null;targetId:string;action:string;outcome:string}
@Injectable({providedIn:'root'}) export class AdministrationService {
 private readonly http=inject(HttpClient);private readonly url='http://localhost:8080/identity/api/users';
 settings(){return this.http.get<DisplaySettings>(this.url+'/administration/settings');}
 display(){return this.http.get<DisplaySettings>(this.url+'/display-settings');}
 save(input:DisplaySettings){return this.http.put<DisplaySettings>(this.url+'/administration/settings',input);}
 audit(from:string,to:string,action:string,target:string,author:string,page=0){return this.http.get(this.url+'/administration/audit',{responseType:'text',params:{from,to,page,...(action?{action}:{}),...(target?{target}:{}),...(author?{author}:{})}}).pipe(map(raw=>{const p=parseDirectory(raw) as AdminPage<AuditEvent>;if(!p||!Array.isArray(p.content)||p.content.length>20||p.number!==page||!Number.isSafeInteger(p.totalElements)||p.totalElements<0||!Number.isSafeInteger(p.totalPages)||p.totalPages<0||p.content.some(e=>!canonicalId(e.id)||!canonicalId(e.targetId)||e.actorId!==null&&!canonicalId(e.actorId)||typeof e.action!=='string'||e.outcome!=='SUCCESS'))throw new HttpErrorResponse({status:502});return p;}));}
 event(id:string){if(!canonicalId(id))throw new Error('Invalid event ID');return this.http.get<AuditEvent>(this.url+'/administration/audit/'+id);}
 revoke(id:string){if(!canonicalId(id))throw new Error('Invalid user ID');return this.http.post(this.url+'/administration/users/'+id+'/revoke-sessions',{});}
}
