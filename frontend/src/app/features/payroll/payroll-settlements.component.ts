import { Component,ChangeDetectionStrategy,DestroyRef,Injector,inject,input,signal,output,effect } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timeout } from 'rxjs';
import { PayrollResponse } from './payroll.models';
import { PayrollService } from './payroll.service';
import { MyPayrollService } from './my-payroll.service';
import { SettlementEntry,SettlementRequest,SettlementSummary,settlementError } from './payroll-settlements.models';

@Component({selector:'app-payroll-settlements',standalone:true,imports:[FormsModule,DatePipe,MatButtonModule],changeDetection:ChangeDetectionStrategy.OnPush,
template:`<section [attr.aria-busy]="busy()"><h2>Règlements enregistrés</h2>
 <button mat-stroked-button (click)="load()" [disabled]="busy()">{{summary() ? 'Actualiser les règlements' : 'Afficher les règlements'}}</button>
 @if(busy()){<p role="status">Opération en cours…</p>}@if(error()){<p role="alert">{{error()}}</p>}
 @if(summary();as s){<p>Total réglé : {{s.paidAmount}} {{payroll().currency}} · Solde : {{s.remainingAmount ?? 'À rapprocher'}} · {{s.state}}</p>
 @if(s.state==='LEGACY_UNRECONCILED'){<p>Statut payé historique sans registre : aucun règlement ni solde confirmé n’est reconstitué automatiquement.</p>}
 @if(s.correctionReason&&!personal()){<p>Correction : {{s.correctionReason}} · auteur {{s.correctedBy}} · {{s.correctedAt | date:'short'}}</p>}
 @if(!s.entries.length){<p>Aucun règlement enregistré.</p>}
 <ol>@for(entry of s.entries;track entry.reference){<li>{{entry.date | date:'dd/MM/yyyy'}} · {{entry.kind}} · {{entry.amount}} · {{entry.method || 'Moyen non renseigné'}} · Réf. {{entry.reference}}
 @if(!personal()){<span> · {{entry.transactionReference}} · auteur {{entry.author}} · {{entry.createdAt | date:'short'}} · {{entry.reason}}</span>
 @if(canReverse(entry)){<button mat-button (click)="reverse(entry)" [disabled]="busy()||!reason.trim()">Contre-écrire ce règlement</button>}}
 </li>}</ol>
 @if(!personal()){
 <label>Motif de contre-écriture ou correction <input maxlength="500" [(ngModel)]="reason" [disabled]="busy()"></label>
 @if(payroll().published&&!payroll().cancelled&&s.state!=='LEGACY_UNRECONCILED'){
 <form (ngSubmit)="pay()"><h3>Enregistrer un règlement manuel</h3>
 <label>Montant <input name="amount" type="number" min="0.01" max="9999999999.99" step="0.01" [(ngModel)]="amount" required></label>
 <label>Date <input name="date" type="date" [(ngModel)]="date" required></label>
 <label>Moyen <input name="method" maxlength="50" [(ngModel)]="method"></label>
 <label>Référence de transaction (facultative) <input name="transaction" maxlength="100" [(ngModel)]="transaction"></label>
 <button mat-flat-button [disabled]="busy()||s.remainingAmount==='0'||s.state==='SETTLED'||s.state==='NO_AMOUNT_DUE'">Enregistrer</button>
 </form><p>Aucun transfert bancaire n’est effectué. Pour une erreur : saisir un motif et ajouter une contre-écriture ; l’original est conservé.</p>
 @if(s.paidAmount==='0'||s.paidAmount==='0.00'){<button mat-stroked-button (click)="correct()" [disabled]="busy()||!reason.trim()">Remplacer par un brouillon correctif</button>}
 }@else if(!payroll().published){<p>Publiez le bulletin après contrôle pour ouvrir les règlements.</p>}
 }}
</section>`})
export class PayrollSettlementsComponent {
 readonly payroll=input.required<PayrollResponse>();readonly personal=input(false);readonly changed=output<void>();
 private readonly injector=inject(Injector);private readonly destroy=inject(DestroyRef);
 protected readonly summary=signal<SettlementSummary|null>(null);protected readonly busy=signal(false);protected readonly error=signal('');
 protected amount:number|null=null;protected date='';protected method='';protected transaction='';protected reason='';
 private pendingKey='';private pendingBody='';
 private readonly session=signal<AuthService|null>(null);private epoch=0;private observed='';
 constructor(){effect(()=>{
  const auth=this.session();const row=this.payroll();const personal=this.personal();if(!auth)return;
  const allowed=auth.isAuthenticated()&&auth.hasAnyRole(personal?[CrmRole.EMPLOYEE]:[CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);
  const identity=auth.sessionGeneration+':'+row.id+':'+personal+':'+allowed;
  if(this.observed&&this.observed!==identity)this.clear();this.observed=identity;
 });}
 private clear(){this.epoch++;this.summary.set(null);this.busy.set(false);this.error.set('');this.pendingKey='';this.pendingBody='';this.amount=null;this.date='';this.method='';this.transaction='';this.reason='';}
 private current(){const auth=this.session()??this.injector.get(AuthService);this.session.set(auth);return {generation:auth.sessionGeneration,id:this.payroll().id,personal:this.personal(),epoch:this.epoch};}
 private valid(context:ReturnType<PayrollSettlementsComponent['current']>){
  const auth=this.session();return !!auth&&auth.isAuthenticated()&&auth.sessionGeneration===context.generation&&this.payroll().id===context.id&&this.personal()===context.personal&&this.epoch===context.epoch&&auth.hasAnyRole(context.personal?[CrmRole.EMPLOYEE]:[CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);
 }
 protected load(){
  if(this.busy())return;const context=this.current();if(!this.valid(context)){this.clear();return;}this.busy.set(true);this.error.set('');
  const request=this.personal()?this.injector.get(MyPayrollService).settlements(String(this.payroll().id)):this.injector.get(PayrollService).settlements(this.payroll().id);
  request.pipe(timeout(10000),takeUntilDestroyed(this.destroy)).subscribe({next:s=>{if(!this.valid(context))return;this.summary.set(s);this.busy.set(false);},error:e=>{if(!this.valid(context))return;this.summary.set(null);this.error.set(settlementError(e));this.busy.set(false);}});
 }
 private key(body:unknown){const text=JSON.stringify(body);if(this.pendingBody!==text||!this.pendingKey){this.pendingBody=text;this.pendingKey=crypto.randomUUID();}return this.pendingKey;}
 protected pay(){
  if(this.personal()||this.busy()||!this.summary()||!this.payroll().published||this.payroll().cancelled)return;
  if(this.amount===null||!Number.isFinite(this.amount)||this.amount<=0||this.amount>9999999999.99||!/^\d+(\.\d{1,2})?$/.test(String(this.amount))||!/^\d{4}-\d{2}-\d{2}$/.test(this.date)){
   this.error.set('Vérifiez le montant positif à deux décimales et la date.');return;
  }
  const body={amount:this.amount,paidAt:this.date,method:this.method.trim()||null,transactionReference:this.transaction.trim()||null};
  const request:SettlementRequest={...body,requestId:this.key({kind:'PAYMENT',...body})};
  this.perform(this.injector.get(PayrollService).pay(this.payroll().id,request));
 }
 protected canReverse(entry:SettlementEntry){return !this.personal()&&!this.payroll().cancelled&&entry.kind==='PAYMENT'&&!this.summary()?.entries.some(r=>r.reversesReference===entry.reference);}
 protected reverse(entry:SettlementEntry){
  if(this.busy()||!this.canReverse(entry)||!this.reason.trim())return;
  const reason=this.reason.trim(),key=this.key({kind:'REVERSAL',reference:entry.reference,reason});
  this.perform(this.injector.get(PayrollService).reverse(this.payroll().id,entry.reference,key,reason));
 }
 private perform(request:import('rxjs').Observable<SettlementSummary>){
  const context=this.current();if(!this.valid(context)){this.clear();return;}this.busy.set(true);this.error.set('');request.pipe(timeout(10000),takeUntilDestroyed(this.destroy)).subscribe({
   next:s=>{if(!this.valid(context))return;this.summary.set(s);this.busy.set(false);this.pendingKey='';this.amount=null;this.changed.emit();},
   error:e=>{if(!this.valid(context))return;this.error.set(settlementError(e));this.busy.set(false);}
  });
 }
 protected correct(){
  if(this.personal()||this.busy()||!this.reason.trim()||!this.payroll().published||this.payroll().cancelled||Number(this.summary()?.paidAmount)!==0||this.summary()?.state==='LEGACY_UNRECONCILED')return;
  const context=this.current();if(!this.valid(context)){this.clear();return;}this.busy.set(true);this.error.set('');this.injector.get(PayrollService).correct(this.payroll().id,this.reason.trim()).pipe(timeout(10000),takeUntilDestroyed(this.destroy)).subscribe({
   next:row=>{if(!this.valid(context))return;this.busy.set(false);void this.injector.get(Router).navigate(['/payroll',row.id,'edit']);},
   error:e=>{if(!this.valid(context))return;this.error.set(settlementError(e));this.busy.set(false);}
  });
 }
}
