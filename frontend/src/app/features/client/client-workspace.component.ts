import { Component, ChangeDetectionStrategy, DestroyRef, inject, signal, computed, effect, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription, forkJoin, of, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ClientService,ClientProfile,ClientInvoice,ClientInvoicePage,clientError } from './client.service';
@Component({
 selector:'app-client-workspace',standalone:true,imports:[DatePipe,MatCardModule,MatButtonModule,FormsModule,MatFormFieldModule,MatInputModule,MatProgressBarModule],
 templateUrl:'./client-workspace.component.html',styleUrl:'./client-workspace.component.scss',
 changeDetection:ChangeDetectionStrategy.OnPush
})
export class ClientWorkspaceComponent {
 private readonly service=inject(ClientService);private readonly auth=inject(AuthService);private readonly destroy=inject(DestroyRef);
 protected readonly query=signal('');
 protected readonly statusFilter=signal('');
 protected readonly statuses=computed(()=>[...new Set(this.page()?.content.map(row=>row.status)??[])].sort());
 protected readonly visibleInvoices=computed(()=>{const query=this.query().trim().toLocaleLowerCase('fr');return (this.page()?.content??[]).filter(row=>(!query||row.invoiceNumber.toLocaleLowerCase('fr').includes(query))&&(!this.statusFilter()||row.status===this.statusFilter()));});
 protected closeDetail(){this.detail.set(null);}
 private requests=new Subscription();private generation=-1;
 protected readonly profile=signal<ClientProfile|null>(null);protected readonly page=signal<ClientInvoicePage|null>(null);
 protected readonly detail=signal<ClientInvoice|null>(null);protected readonly error=signal('');protected readonly loading=signal(false);protected readonly busy=signal(false);
 constructor(){
  effect(()=>{this.auth.getCurrentUser();const allowed=this.auth.hasAnyRole([CrmRole.CLIENT]);const g=this.auth.sessionGeneration;
   untracked(()=>{if(g!==this.generation || !allowed){
    this.requests.unsubscribe();this.requests=new Subscription();this.generation=g;
    this.profile.set(null);this.page.set(null);this.detail.set(null);this.query.set('');this.statusFilter.set('');this.error.set('');this.loading.set(false);this.busy.set(false);
    if(allowed)this.load(0);
   }});
  });this.destroy.onDestroy(()=>this.requests.unsubscribe());
 }
 private current(g:number){return g===this.auth.sessionGeneration && !this.destroy.destroyed && this.auth.hasAnyRole([CrmRole.CLIENT]);}
 protected load(page:number){
  if(this.loading()||this.busy()||!Number.isSafeInteger(page)||page<0||!this.current(this.generation))return;const g=this.generation;
  this.loading.set(true);this.error.set('');this.detail.set(null);
  this.requests.add(forkJoin({profile:this.profile()?of(this.profile()!):this.service.profile(),page:this.service.invoices(page)}).pipe(timeout(10000)).subscribe({
   next:r=>{if(this.current(g)){this.profile.set(r.profile);this.page.set(r.page);this.query.set('');this.statusFilter.set('');this.loading.set(false);}},
   error:e=>{if(this.current(g)){this.profile.set(null);this.page.set(null);this.error.set(clientError(e));this.loading.set(false);}}
  }));
 }
 protected open(id:string){
  if(this.busy()||!this.current(this.generation))return;const g=this.generation;this.busy.set(true);this.detail.set(null);this.error.set('');
  this.requests.add(this.service.detail(id).pipe(timeout(10000)).subscribe({
   next:r=>{if(this.current(g)){this.detail.set(r);this.busy.set(false);}},
   error:e=>{if(this.current(g)){this.error.set(clientError(e));this.busy.set(false);}}
  }));
 }
 protected download(id:string){
  if(this.busy()||!this.current(this.generation))return;const g=this.generation;this.busy.set(true);this.error.set('');
  this.requests.add(this.service.pdf(id).pipe(timeout(15000)).subscribe({
   next:blob=>{if(this.current(g)){const url=URL.createObjectURL(blob);const a=document.createElement('a');
    a.href=url;a.download='facture-'+id+'.pdf';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);this.busy.set(false);}},
   error:e=>{if(this.current(g)){this.error.set(clientError(e));this.busy.set(false);}}
  }));
 }
}
