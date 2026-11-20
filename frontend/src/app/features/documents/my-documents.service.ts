import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { map } from 'rxjs';
import { canonicalId } from '../users/association-identifiers';

export interface PersonalDocument { id:string; name:string; type:string; mediaType:string; size:number; createdAt:string }
@Injectable({providedIn:'root'})
export class MyDocumentsService {
  private readonly http=inject(HttpClient);
  private readonly base='http://localhost:8080/hr/api/documents/me';
  list(){return this.http.get<PersonalDocument[]>(this.base).pipe(map(rows=>{
    if(!Array.isArray(rows)||rows.some(d=>!d||!canonicalId(d.id)||typeof d.name!=='string'||!d.name||typeof d.type!=='string'
      ||!['application/pdf','image/png','image/jpeg'].includes(d.mediaType)||!Number.isSafeInteger(d.size)||d.size<1||d.size>10485760
      ||typeof d.createdAt!=='string'))throw new HttpErrorResponse({status:502});
    return rows;
  }));}
  download(document:PersonalDocument){
    if(!canonicalId(document.id))throw new Error('Invalid identifier');
    return this.http.get(this.base+'/'+document.id+'/content',{observe:'response',responseType:'blob'}).pipe(map(response=>{
      if(!response.body?.size||response.body.size>10485760||response.headers.get('Content-Type')?.split(';')[0]!==document.mediaType)
        throw new HttpErrorResponse({status:502});
      return response.body;
    }));
  }
}
