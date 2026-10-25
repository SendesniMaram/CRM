import { HttpClient, HttpErrorResponse, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { canonicalId, parseDirectory } from '../users/association-identifiers';
import { PDF_DOWNLOAD_REQUEST } from '../../core/http/pdf-download-context';
export interface ClientProfile { customerId: string; name: string; email: string|null; phone: string|null; company: string|null }
export interface ClientInvoice { id: string; invoiceNumber: string; issueDate: string|null; dueDate: string|null; totalAmount: string; currency: string|null; status: string }
export interface ClientInvoicePage { content: ClientInvoice[]; totalElements: number; totalPages: number; number: number }
export function validInvoice(r: ClientInvoice) {
 return !!r && canonicalId(r.id) && typeof r.invoiceNumber==='string' && typeof r.status==='string'
  && typeof r.totalAmount==='string' && /^-?\d+(\.\d+)?$/.test(r.totalAmount)
  && (r.currency===null || typeof r.currency==='string');
}
@Injectable({providedIn:'root'})
export class ClientService {
 private readonly http=inject(HttpClient);
 private readonly customer='http://localhost:8080/customer/api/customers';
 private readonly invoice='http://localhost:8080/invoice/api/invoices/me';
 profile(){return this.http.get<ClientProfile>(this.customer+'/me').pipe(map(r=>{
  if(!r || !canonicalId(r.customerId) || typeof r.name!=='string'
   || [r.email,r.phone,r.company].some(v=>v!==null && typeof v!=='string')) throw new HttpErrorResponse({status:502});
  return r;
 }));}
 invoices(page=0){return this.http.get<ClientInvoicePage>(this.invoice,{params:{page,size:10}}).pipe(map(r=>{
  if(!r || !Array.isArray(r.content) || !Number.isSafeInteger(r.totalElements) || r.totalElements<0
   || !Number.isSafeInteger(r.totalPages) || r.totalPages<0 || r.number!==page || r.content.some(x=>!validInvoice(x))) throw new HttpErrorResponse({status:502});
  return r;
 }));}
 detail(id:string){this.id(id);return this.http.get<ClientInvoice>(this.invoice+'/'+id).pipe(map(r=>{
  if(!validInvoice(r) || r.id!==id) throw new HttpErrorResponse({status:502});return r;
 }));}
 pdf(id:string){this.id(id);return this.http.get(this.invoice+'/'+id+'/pdf',{
  responseType:'blob',observe:'response',context:new HttpContext().set(PDF_DOWNLOAD_REQUEST,true)
 }).pipe(map(r=>{if(!r.body?.size || r.headers.get('Content-Type')?.split(';')[0]!=='application/pdf')throw new HttpErrorResponse({status:502});return r.body;}));}
 users(){return this.http.get('http://localhost:8080/identity/api/users',{responseType:'text'}).pipe(map(t=>{
  const rows=parseDirectory(t) as {id:string;username:string;roles:string[];enabled:boolean;accountNonLocked:boolean}[];
  if(!Array.isArray(rows) || rows.some(r=>!canonicalId(r.id)||!Array.isArray(r.roles)))throw new HttpErrorResponse({status:502});
  return rows.filter(r=>r.roles.includes('CLIENT')&&r.enabled===true&&r.accountNonLocked===true);
 }));}
 customers(){return this.http.get(this.customer,{responseType:'text'}).pipe(map(t=>{
  const rows=parseDirectory(t) as {id:string;name:string}[];
  if(!Array.isArray(rows)||rows.some(r=>!canonicalId(r.id)||typeof r.name!=='string'))throw new HttpErrorResponse({status:502});return rows;
 }));}
 association(uid:string){this.id(uid);return this.http.get<{userId:string;customerId:string}>(this.customer+'/associations/users/'+uid);}
 associate(userId:string,customerId:string){this.id(userId);this.id(customerId);return this.http.post<{userId:string;customerId:string}>(this.customer+'/associations',{userId,customerId});}
 private id(id:string){if(!canonicalId(id))throw new Error('Invalid identifier');}
}
export function clientError(e:{status?:number}) {
 return ({400:'Demande invalide.',401:'Votre session a expiré. Reconnectez-vous.',403:'Accès non autorisé.',
 404:'Aucune association ou fiche accessible. Contactez un administrateur.',409:'Association en conflit : aucune réaffectation autorisée.',
 502:'Réponse du service invalide.',503:'Service temporairement indisponible.'} as Record<number,string>)[e.status??0]??'Erreur de communication. Réessayez.';
}
