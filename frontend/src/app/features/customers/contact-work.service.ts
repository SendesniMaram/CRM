import { inject,Injectable } from '@angular/core';import { HttpClient,HttpErrorResponse } from '@angular/common/http';import { map,Observable } from 'rxjs';import { parseDirectory,canonicalId } from '../users/association-identifiers';
export interface WorkPage<T>{content:T[];number:number;totalPages:number;totalElements:number}
export interface AssignedContact{id:string;name:string;email:string|null;phone:string|null;address:string|null;company:string|null;contactType:string}
export interface AssignedInteraction{id:string;interactionType:string;occurredAt:string;notes:string}
export interface ClientRequest{id:string;customerId:string|null;subject:string;description:string;status:string;createdAt:string|null}
export interface ClientMessage{id:string;senderKind:string;body:string;createdAt:string|null}
export interface RequestEvent{id:string;status:string;createdAt:string|null}
export type CaseSpace='admin'|'client';
export function commercialError(e:{status?:number}){return ({400:'Vérifiez les champs et les paramètres.',401:'Session expirée.',403:'Accès non autorisé.',404:'Fiche, demande ou association introuvable.',409:'Opération refusée : archive, doublon ou demande clôturée. Actualisez.',502:'Réponse du service invalide.',503:'Service de référence indisponible.'} as Record<number,string>)[e.status??0]??'Opération indisponible. Vérifiez les données avant une reprise.';}
@Injectable({providedIn:'root'}) export class ContactWorkService {private readonly http=inject(HttpClient);private readonly url='http://localhost:8080/customer/api/contact-work';private id(id:string){if(!canonicalId(id))throw new Error('Invalid identifier');}
private parse<T>(source:Observable<string>,valid:(v:T)=>boolean){return source.pipe(map(raw=>{const v=parseDirectory(raw) as T;if(!v||!valid(v))throw new HttpErrorResponse({status:502});return v;}));}
private page<T>(source:Observable<string>,page:number,valid:(v:T)=>boolean){return this.parse<WorkPage<T>>(source,v=>Array.isArray(v.content)&&v.content.every(valid)&&v.number===page&&Number.isSafeInteger(v.totalPages)&&v.totalPages>=0&&Number.isSafeInteger(v.totalElements)&&v.totalElements>=0);}
private contact=(v:AssignedContact)=>!!v&&canonicalId(v.id)&&typeof v.name==='string';private request=(v:ClientRequest)=>!!v&&canonicalId(v.id)&&typeof v.subject==='string'&&typeof v.description==='string'&&['OPEN','IN_PROGRESS','RESOLVED'].includes(v.status);
assigned(keyword='',page=0){return this.page<AssignedContact>(this.http.get(this.url+'/assigned',{params:{keyword,page},responseType:'text'}),page,this.contact);}
contactDetail(id:string){this.id(id);return this.parse<AssignedContact>(this.http.get(this.url+'/assigned/'+id,{responseType:'text'}),this.contact);}
editContact(id:string,body:{email:string|null;phone:string|null;address:string|null}){this.id(id);return this.parse<AssignedContact>(this.http.put(this.url+'/assigned/'+id,body,{responseType:'text'}),this.contact);}
interactions(id:string,page=0){this.id(id);return this.page<AssignedInteraction>(this.http.get(this.url+'/assigned/'+id+'/interactions',{params:{page},responseType:'text'}),page,v=>!!v&&canonicalId(v.id)&&typeof v.notes==='string');}
addInteraction(id:string,body:{interactionType:string;occurredAt:string;notes:string}){this.id(id);return this.parse<AssignedInteraction>(this.http.post(this.url+'/assigned/'+id+'/interactions',body,{responseType:'text'}),v=>!!v&&canonicalId(v.id));}
cases(space:CaseSpace,status='',page=0){return this.page<ClientRequest>(this.http.get(this.url+'/'+space+'/cases',{params:{page,...(status?{status}:{})},responseType:'text'}),page,this.request);}
caseDetail(space:CaseSpace,id:string){this.id(id);return this.parse<ClientRequest>(this.http.get(this.url+'/'+space+'/cases/'+id,{responseType:'text'}),this.request);}
createCase(body:{requestId:string;subject:string;description:string}){return this.parse<ClientRequest>(this.http.post(this.url+'/client/cases',body,{responseType:'text'}),this.request);}
messages(space:CaseSpace,id:string,page=0){this.id(id);return this.page<ClientMessage>(this.http.get(this.url+'/'+space+'/cases/'+id+'/messages',{params:{page},responseType:'text'}),page,v=>!!v&&canonicalId(v.id)&&['TEAM','CLIENT'].includes(v.senderKind)&&typeof v.body==='string');}
history(space:CaseSpace,id:string,page=0){this.id(id);return this.page<RequestEvent>(this.http.get(this.url+'/'+space+'/cases/'+id+'/history',{params:{page},responseType:'text'}),page,v=>!!v&&canonicalId(v.id)&&['OPEN','IN_PROGRESS','RESOLVED'].includes(v.status));}
send(space:CaseSpace,id:string,body:{requestId:string;body:string}){this.id(id);return this.parse<ClientMessage>(this.http.post(this.url+'/'+space+'/cases/'+id+'/messages',body,{responseType:'text'}),v=>!!v&&canonicalId(v.id));}
state(id:string,status:string){this.id(id);return this.parse<ClientRequest>(this.http.patch(this.url+'/admin/cases/'+id+'/status',{status},{responseType:'text'}),this.request);}
}
