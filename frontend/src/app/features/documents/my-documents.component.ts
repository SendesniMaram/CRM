import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Subscription, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { clientError } from '../client/client.service';
import { MyDocumentsService, PersonalDocument } from './my-documents.service';

@Component({selector:'app-my-documents',standalone:true,imports:[DatePipe,MatButtonModule,MatCardModule],changeDetection:ChangeDetectionStrategy.OnPush,
 template:`<mat-card><mat-card-content><h1>Mes documents</h1><p>Documents partagés avec vous par votre administrateur.</p>
 <button mat-stroked-button (click)="load()" [disabled]="loading()||busy()">Actualiser</button>
 @if(loading()){<p role="status">Chargement des documents…</p>}
 @if(busy()){<p role="status">Téléchargement en cours…</p>}
 @if(error()){<p role="alert">{{error()}}</p>}
 @if(rows();as documents){@if(!documents.length){<p role="status">Aucun document partagé.</p>}
 <ul>@for(document of documents;track document.id){<li>{{document.name}} · {{document.type}} · {{document.createdAt | date:'dd/MM/yyyy'}} · {{document.size}} octets
 <button mat-button (click)="download(document)" [disabled]="busy()||loading()" [attr.aria-label]="'Télécharger '+document.name">Télécharger</button></li>}</ul>}
 </mat-card-content></mat-card>`})
export class MyDocumentsComponent {
 private readonly service=inject(MyDocumentsService);
 private readonly auth=inject(AuthService);
 private readonly destroy=inject(DestroyRef);
 private requests=new Subscription();private generation=-1;
 protected readonly rows=signal<PersonalDocument[]|null>(null);
 protected readonly error=signal('');protected readonly loading=signal(false);protected readonly busy=signal(false);
 constructor(){
  effect(()=>{this.auth.getCurrentUser();const allowed=this.auth.hasAnyRole([CrmRole.EMPLOYEE]);const generation=this.auth.sessionGeneration;
   untracked(()=>{if(generation!==this.generation||!allowed){
    this.requests.unsubscribe();this.requests=new Subscription();this.generation=generation;
    this.rows.set(null);this.error.set('');this.loading.set(false);this.busy.set(false);if(allowed)this.load();
   }});
  });this.destroy.onDestroy(()=>this.requests.unsubscribe());
 }
 private current(g:number){return !this.destroy.destroyed&&g===this.auth.sessionGeneration&&this.auth.hasAnyRole([CrmRole.EMPLOYEE]);}
 protected load(){
  if(this.loading()||this.busy()||!this.current(this.generation))return;
  const generation=this.generation;this.loading.set(true);this.error.set('');this.rows.set(null);
  this.requests.add(this.service.list().pipe(timeout(10000)).subscribe({
   next:rows=>{if(this.current(generation)){this.rows.set(rows);this.loading.set(false);}},
   error:e=>{if(this.current(generation)){this.error.set(clientError(e));this.loading.set(false);}}
  }));
 }
 protected download(document:PersonalDocument){
  if(this.busy()||this.loading()||!this.current(this.generation))return;
  const generation=this.generation;this.busy.set(true);this.error.set('');
  this.requests.add(this.service.download(document).pipe(timeout(15000)).subscribe({
   next:blob=>{if(this.current(generation)){
    const url=URL.createObjectURL(blob),anchor=globalThis.document.createElement('a');anchor.href=url;anchor.download=document.name;anchor.click();
    setTimeout(()=>URL.revokeObjectURL(url),1000);this.busy.set(false);
   }},
   error:e=>{if(this.current(generation)){this.error.set(clientError(e));this.busy.set(false);}}
  }));
 }
}
