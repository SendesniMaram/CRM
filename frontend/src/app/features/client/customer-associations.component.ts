import { Component,ChangeDetectionStrategy,DestroyRef,inject,signal,effect,untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { forkJoin,Subscription } from 'rxjs';
import { ClientService,clientError } from './client.service';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
@Component({standalone:true,imports:[FormsModule,MatCardModule,MatButtonModule],
 template:`<h1>Associations comptes clients</h1><mat-card><mat-card-content>
 <p>Association explicite et définitive. Aucun transfert d'historique ni réaffectation.</p>
 @if(error()){<p role="alert">{{error()}}</p>}@if(message()){<p role="status">{{message()}}</p>}
 <label>Compte CLIENT <select [(ngModel)]="uid" (ngModelChange)="lookup()" [disabled]="busy()"><option value="">Sélectionner</option>
 @for(u of users();track u.id){<option [value]="u.id">{{u.username}} ({{u.id}})</option>}</select></label>
 <label>Fiche Customer <select [(ngModel)]="cid" [disabled]="busy()"><option value="">Sélectionner</option>
 @for(c of customers();track c.id){<option [value]="c.id">{{c.name}} ({{c.id}})</option>}</select></label>
 <button mat-flat-button (click)="create()" [disabled]="busy() || !uid || !cid || linked()">Associer</button>
 <button mat-button (click)="load()" [disabled]="busy()">Actualiser les références</button>
 </mat-card-content></mat-card>`,changeDetection:ChangeDetectionStrategy.OnPush})
export class CustomerAssociationsComponent {
 private readonly service=inject(ClientService);private readonly auth=inject(AuthService);private readonly destroy=inject(DestroyRef);
 private requests=new Subscription();private generation=-1;private lookupVersion=0;
 protected readonly users=signal<{id:string;username:string}[]>([]);protected readonly customers=signal<{id:string;name:string}[]>([]);
 protected readonly busy=signal(false);protected readonly linked=signal(true);protected readonly error=signal('');protected readonly message=signal('');
 protected uid='';protected cid='';
 constructor(){effect(()=>{this.auth.getCurrentUser();const g=this.auth.sessionGeneration;const admin=this.admin();
  untracked(()=>{if(g!==this.generation||!admin){this.requests.unsubscribe();this.requests=new Subscription();this.generation=g;
   this.users.set([]);this.customers.set([]);this.uid='';this.cid='';this.linked.set(true);this.busy.set(false);this.message.set('');this.error.set('');
   if(admin)this.load();
  }});
 });this.destroy.onDestroy(()=>this.requests.unsubscribe());}
 private admin(){return this.auth.hasAnyRole([CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);}
 private current(g:number){return g===this.auth.sessionGeneration&&this.admin()&&!this.destroy.destroyed;}
 protected load(){if(this.busy()||!this.admin())return;const g=this.generation;this.busy.set(true);this.error.set('');
 this.requests.add(forkJoin({users:this.service.users(),customers:this.service.customers()}).subscribe({
  next:r=>{if(this.current(g)){this.users.set(r.users);this.customers.set(r.customers);this.busy.set(false);}},
  error:e=>{if(this.current(g)){this.error.set(clientError(e));this.busy.set(false);}}
 }));}
 protected lookup(){const g=this.generation;const uid=this.uid;const version=++this.lookupVersion;this.linked.set(true);this.message.set('');this.error.set('');
 if(!uid||!this.current(g))return;
 const current=()=>this.current(g)&&this.uid===uid&&version===this.lookupVersion;
 this.requests.add(this.service.association(uid).subscribe({
  next:r=>{if(current())this.message.set('Déjà associé à Customer '+r.customerId+'.');},
  error:e=>{if(current()){if(e.status===404){this.linked.set(false);this.message.set('Compte non associé.');}else this.error.set(clientError(e));}}
 }));}
 protected create(){if(this.busy()||this.linked()||!this.uid||!this.cid||!this.current(this.generation))return;
 const g=this.generation;this.busy.set(true);this.error.set('');
 this.requests.add(this.service.associate(this.uid,this.cid).subscribe({
  next:r=>{if(this.current(g)){this.busy.set(false);this.linked.set(true);this.message.set('Association enregistrée : Customer '+r.customerId+'.');}},
  error:e=>{if(this.current(g)){this.busy.set(false);this.error.set(clientError(e));if(e.status===409)this.linked.set(true);}}
 }));}
}
