import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, effect, untracked, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin,Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { LeaveBalanceComponent } from './leave-balance.component';
import { DepartmentService } from '../departments/services/department.service';
import { DepartmentResponse, EmployeeResponse } from '../employees/models/employee.models';
import { EmployeeService } from '../employees/services/employee.service';
import { LeaveResponse } from './leave.models';
import { LeaveCalendarService } from './leave-calendar.service';
interface Quota {id:number;employeeId:string;periodStart:string;periodEnd:string;leaveType:string;grantedDays:number;}

@Component({ selector: 'app-leave-calendar', standalone: true, imports: [DatePipe, FormsModule,LeaveBalanceComponent],
  template: `<main class="container py-4"><h1>Calendrier des congés</h1>
    <p>Les périodes et statuts sont issus des demandes. Les soldes techniques sont consultables ci-dessous ; aucun calcul de jours ouvrés n’est appliqué.</p>
    <div class="d-flex gap-2 align-items-center flex-wrap mb-3">
      <button type="button" class="btn btn-outline-primary" (click)="move(-1)">Mois précédent</button><strong>{{ monthLabel() }}</strong>
      <button type="button" class="btn btn-outline-primary" (click)="move(1)">Mois suivant</button>
      <label>Employé <input class="form-control" [(ngModel)]="employeeId" maxlength="50"></label>
      <label>Statut <select class="form-select" [(ngModel)]="status"><option value="">Tous</option><option value="PENDING">En attente</option><option value="APPROVED">Approuvées</option><option value="REJECTED">Refusées</option></select></label>
      <label>Département <select class="form-select" [(ngModel)]="departmentId"><option value="">Tous</option>@for (d of departments(); track d.id) { <option [value]="d.id">{{ d.name }}</option> }</select></label>
      <button type="button" class="btn btn-primary" (click)="load()" [disabled]="loading()">Filtrer</button>
    </div>
    @if (error()) { <p role="alert" class="text-danger">{{ error() }}</p> }
    @if (loading()) { <p role="status">Chargement…</p> }
    @if (!loading() && !error() && visibleEntries().length===0) { <p>Aucune demande sur cette période.</p> }
    @if (visibleEntries().length) { <table class="table table-striped"><thead><tr><th>Employé</th><th>Début</th><th>Fin</th><th>Statut</th><th>Motif</th></tr></thead><tbody>
      @for (entry of visibleEntries(); track entry.id) { <tr><td>{{ entry.employeeId }} {{ employeeName(entry.employeeId) }}</td><td>{{ entry.startDate | date:'mediumDate' }}</td><td>{{ entry.endDate | date:'mediumDate' }}</td><td>{{ entry.status }}</td><td>{{ entry.reason || '—' }}</td></tr> }
    </tbody></table> }
    <section class="card mt-4"><div class="card-body"><h2>Attribution administrative de quotas</h2><p>Les jours attribués sont enregistrés explicitement. Le solde technique doit être consulté sur la période exacte du quota.</p>
      <div class="row g-2"><label class="col-md-2">Employé <input class="form-control" [(ngModel)]="quotaEmployee" (blur)="loadQuotas()"></label><label class="col-md-2">Type <input class="form-control" [(ngModel)]="quotaType" maxlength="50"></label><label class="col-md-2">Du <input class="form-control" type="date" [(ngModel)]="quotaFrom"></label><label class="col-md-2">Au <input class="form-control" type="date" [(ngModel)]="quotaTo"></label><label class="col-md-2">Jours attribués <input class="form-control" type="number" min="0" step="0.5" [(ngModel)]="quotaDays"></label><div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary" (click)="assignQuota()" [disabled]="loading()">Attribuer</button></div></div>
      @for(q of quotas();track q.id){<p class="mt-2">Employé {{q.employeeId}} · {{q.leaveType}} · {{q.periodStart}} – {{q.periodEnd}} · {{q.grantedDays}} jours attribués <button class="btn btn-sm btn-outline-danger" (click)="deleteQuota(q)">Supprimer</button></p>}
    </div></section><app-leave-balance [personal]="false" [refresh]="balanceRefresh()" /></main>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class LeaveCalendarComponent implements OnInit {
  private readonly auth=inject(AuthService);private readonly destroy=inject(DestroyRef);private request?:Subscription;private revision=0;private quotaRevision=0;private generation=this.auth.sessionGeneration;
  protected status='';protected readonly balanceRefresh=signal(0);
  constructor(){effect(()=>{this.auth.getCurrentUser();const generation=this.auth.sessionGeneration;const allowed=this.admin();untracked(()=>{if(generation!==this.generation||!allowed){this.generation=generation;this.revision++;this.quotaRevision++;this.request?.unsubscribe();this.entries.set([]);this.employeeDirectory.set([]);this.departments.set([]);this.quotas.set([]);this.employeeId='';this.departmentId='';this.quotaEmployee='';this.loading.set(false);this.error.set('');}});});}
  ngOnInit(){this.load();}
  private admin(){return this.auth.hasAnyRole([CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);}
  private current(revision:number,generation:number){return revision===this.revision&&generation===this.auth.sessionGeneration&&this.admin()&&!this.destroy.destroyed;}
  private readonly service=inject(LeaveCalendarService); private readonly employeesService=inject(EmployeeService); private readonly departmentsService=inject(DepartmentService); private readonly http=inject(HttpClient);
  protected readonly entries=signal<LeaveResponse[]>([]); protected readonly departments=signal<DepartmentResponse[]>([]);
  private readonly employeeDirectory=signal<EmployeeResponse[]>([]); protected readonly loading=signal(false); protected readonly error=signal('');
  protected readonly quotas=signal<Quota[]>([]);protected quotaEmployee='';protected quotaType='ANNUAL';protected quotaFrom='';protected quotaTo='';protected quotaDays=0;
  protected month=new Date(new Date().getFullYear(),new Date().getMonth(),1); protected employeeId=''; protected departmentId='';
  protected monthLabel(){return new Intl.DateTimeFormat('fr-FR',{month:'long',year:'numeric'}).format(this.month);}
  protected move(amount:number){const next=new Date(this.month);next.setDate(1);next.setMonth(next.getMonth()+amount);if(next.getFullYear()<1||next.getFullYear()>9998){this.error.set('Année hors limites.');return;}this.month=next;this.load();}
  protected visibleEntries(){const rows=this.entries().filter(e=>!this.status||e.status===this.status);const id=this.departmentId;if(!id)return rows;const allowed=new Set(this.employeeDirectory().filter(e=>String(e.departmentId??'')===id).map(e=>String(e.id)));return rows.filter(e=>allowed.has(e.employeeId));}
  protected employeeName(id:string){const e=this.employeeDirectory().find(x=>String(x.id)===id);return e?`${e.firstName??''} ${e.lastName??''}`.trim():'';}
  protected assignQuota(){
    if(!this.admin())return;if(!/^[1-9][0-9]{0,18}$/.test(this.quotaEmployee)||!this.quotaType.trim()||!this.quotaFrom||!this.quotaTo||this.quotaTo<this.quotaFrom||!Number.isFinite(this.quotaDays)||this.quotaDays<0||this.quotaDays>99999.99||!/^\d+(\.\d{1,2})?$/.test(String(this.quotaDays))){this.error.set('Vérifiez les informations du quota.');return;}
    const generation=this.auth.sessionGeneration,employee=this.quotaEmployee;this.error.set('');
    this.http.post('http://localhost:8080/hr/api/leave-quotas',{employeeId:employee,leaveType:this.quotaType.trim(),periodStart:this.quotaFrom,periodEnd:this.quotaTo,grantedDays:this.quotaDays}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{if(generation===this.auth.sessionGeneration&&employee===this.quotaEmployee&&this.admin()){this.loadQuotas();this.balanceRefresh.update(v=>v+1);}},error:()=>{if(generation===this.auth.sessionGeneration&&this.admin())this.error.set('Attribution du quota impossible.');}});
  }
  protected deleteQuota(q:Quota){if(!this.admin())return;const generation=this.auth.sessionGeneration;this.http.delete('http://localhost:8080/hr/api/leave-quotas/'+q.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{if(generation===this.auth.sessionGeneration&&this.admin()){this.loadQuotas();this.balanceRefresh.update(v=>v+1);}},error:()=>{if(generation===this.auth.sessionGeneration&&this.admin())this.error.set('Suppression du quota impossible.');}});}
  protected loadQuotas(){const revision=++this.quotaRevision,generation=this.auth.sessionGeneration,employee=this.quotaEmployee;this.quotas.set([]);if(!this.admin()||!/^[1-9][0-9]{0,18}$/.test(employee))return;
    this.http.get<Quota[]>('http://localhost:8080/hr/api/employees/'+employee+'/leave-quotas').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:q=>{if(revision===this.quotaRevision&&generation===this.auth.sessionGeneration&&this.admin()&&employee===this.quotaEmployee)this.quotas.set(q);},error:()=>{if(revision===this.quotaRevision&&generation===this.auth.sessionGeneration&&this.admin())this.error.set('Impossible de charger les quotas.');}});
  }
  protected load(){
    const revision=++this.revision,generation=this.auth.sessionGeneration;this.request?.unsubscribe();this.entries.set([]);this.loading.set(false);if(!this.admin())return;
    if(this.employeeId.trim()&&!/^[1-9][0-9]{0,18}$/.test(this.employeeId.trim())){this.error.set('Identifiant employé invalide.');return;}
    const last=new Date(this.month);last.setMonth(last.getMonth()+1,0);const from=this.date(this.month),to=this.date(last);this.loading.set(true);this.error.set('');
    this.request=forkJoin({rows:this.service.list(from,to,this.employeeId),employees:this.employeesService.getEmployees(),departments:this.departmentsService.getDepartments()}).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next:r=>{if(!this.current(revision,generation))return;this.entries.set(r.rows);this.employeeDirectory.set(r.employees);this.departments.set(r.departments);this.loading.set(false);},
      error:(e:HttpErrorResponse)=>{if(!this.current(revision,generation))return;this.entries.set([]);this.employeeDirectory.set([]);this.loading.set(false);this.error.set(e.status===403?'Accès réservé à l’administration.':e.status===401?'Session expirée.':'Impossible de charger le calendrier ou les annuaires Employé/Département.');}
    });
  }
  private date(v:Date){return [String(v.getFullYear()).padStart(4,'0'),String(v.getMonth()+1).padStart(2,'0'),String(v.getDate()).padStart(2,'0')].join('-');}
}
