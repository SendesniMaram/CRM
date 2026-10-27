import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { DepartmentService } from '../departments/services/department.service';
import { DepartmentResponse, EmployeeResponse } from '../employees/models/employee.models';
import { EmployeeService } from '../employees/services/employee.service';
import { LeaveResponse } from './leave.models';
import { LeaveCalendarService } from './leave-calendar.service';
interface Quota {id:number;employeeId:string;periodStart:string;periodEnd:string;leaveType:string;grantedDays:number;}

@Component({ selector: 'app-leave-calendar', standalone: true, imports: [DatePipe, FormsModule],
  template: `<main class="container py-4"><h1>Calendrier des congés</h1>
    <p>Les périodes et statuts sont issus des demandes. Aucun solde ni calcul de jours ouvrés n’est appliqué.</p>
    <div class="d-flex gap-2 align-items-center flex-wrap mb-3">
      <button type="button" class="btn btn-outline-primary" (click)="move(-1)">Mois précédent</button><strong>{{ monthLabel() }}</strong>
      <button type="button" class="btn btn-outline-primary" (click)="move(1)">Mois suivant</button>
      <label>Employé <input class="form-control" [(ngModel)]="employeeId" maxlength="50"></label>
      <label>Département <select class="form-select" [(ngModel)]="departmentId"><option value="">Tous</option>@for (d of departments(); track d.id) { <option [value]="d.id">{{ d.name }}</option> }</select></label>
      <button type="button" class="btn btn-primary" (click)="load()" [disabled]="loading()">Filtrer</button>
    </div>
    @if (error()) { <p role="alert" class="text-danger">{{ error() }}</p> }
    @if (loading()) { <p role="status">Chargement…</p> }
    @if (!loading() && !error() && visibleEntries().length===0) { <p>Aucune demande sur cette période.</p> }
    @if (visibleEntries().length) { <table class="table table-striped"><thead><tr><th>Employé</th><th>Début</th><th>Fin</th><th>Statut</th><th>Motif</th></tr></thead><tbody>
      @for (entry of visibleEntries(); track entry.id) { <tr><td>{{ entry.employeeId }} {{ employeeName(entry.employeeId) }}</td><td>{{ entry.startDate | date:'mediumDate' }}</td><td>{{ entry.endDate | date:'mediumDate' }}</td><td>{{ entry.status }}</td><td>{{ entry.reason || '—' }}</td></tr> }
    </tbody></table> }
    <section class="card mt-4"><div class="card-body"><h2>Attribution administrative de quotas</h2><p>Les jours attribués sont enregistrés explicitement. Aucun solde consommé ou restant n’est calculé.</p>
      <div class="row g-2"><label class="col-md-2">Employé <input class="form-control" [(ngModel)]="quotaEmployee" (blur)="loadQuotas()"></label><label class="col-md-2">Type <input class="form-control" [(ngModel)]="quotaType" maxlength="50"></label><label class="col-md-2">Du <input class="form-control" type="date" [(ngModel)]="quotaFrom"></label><label class="col-md-2">Au <input class="form-control" type="date" [(ngModel)]="quotaTo"></label><label class="col-md-2">Jours attribués <input class="form-control" type="number" min="0" step="0.5" [(ngModel)]="quotaDays"></label><div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary" (click)="assignQuota()" [disabled]="loading()">Attribuer</button></div></div>
      @for(q of quotas();track q.id){<p class="mt-2">Employé {{q.employeeId}} · {{q.leaveType}} · {{q.periodStart}} – {{q.periodEnd}} · {{q.grantedDays}} jours attribués <button class="btn btn-sm btn-outline-danger" (click)="deleteQuota(q)">Supprimer</button></p>}
    </div></section></main>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class LeaveCalendarComponent {
  private readonly service=inject(LeaveCalendarService); private readonly employeesService=inject(EmployeeService); private readonly departmentsService=inject(DepartmentService); private readonly http=inject(HttpClient);
  protected readonly entries=signal<LeaveResponse[]>([]); protected readonly departments=signal<DepartmentResponse[]>([]);
  private readonly employeeDirectory=signal<EmployeeResponse[]>([]); protected readonly loading=signal(false); protected readonly error=signal('');
  protected readonly quotas=signal<Quota[]>([]);protected quotaEmployee='';protected quotaType='ANNUAL';protected quotaFrom='';protected quotaTo='';protected quotaDays=0;
  protected month=new Date(new Date().getFullYear(),new Date().getMonth(),1); protected employeeId=''; protected departmentId='';
  protected monthLabel(){return new Intl.DateTimeFormat('fr-FR',{month:'long',year:'numeric'}).format(this.month);}
  protected move(amount:number){this.month=new Date(this.month.getFullYear(),this.month.getMonth()+amount,1);this.load();}
  protected visibleEntries(){const id=this.departmentId;if(!id)return this.entries();const allowed=new Set(this.employeeDirectory().filter(e=>String(e.departmentId??'')===id).map(e=>String(e.id)));return this.entries().filter(e=>allowed.has(e.employeeId));}
  protected employeeName(id:string){const e=this.employeeDirectory().find(x=>String(x.id)===id);return e?`${e.firstName??''} ${e.lastName??''}`.trim():'';}
  protected assignQuota(){if(!/^[1-9][0-9]{0,18}$/.test(this.quotaEmployee)||!this.quotaType.trim()||!this.quotaFrom||!this.quotaTo||this.quotaTo<this.quotaFrom||this.quotaDays<0){this.error.set('Vérifiez les informations du quota.');return;}this.loading.set(true);this.error.set('');this.http.post('http://localhost:8080/hr/api/leave-quotas',{employeeId:this.quotaEmployee,leaveType:this.quotaType.trim(),periodStart:this.quotaFrom,periodEnd:this.quotaTo,grantedDays:this.quotaDays}).subscribe({next:()=>{this.loading.set(false);this.loadQuotas();},error:(e:HttpErrorResponse)=>{this.loading.set(false);this.error.set(e.status===404?'Employé introuvable.':e.status===503?'Employee Service indisponible ou non configuré.':'Attribution du quota impossible.');}});}
  protected deleteQuota(q:Quota){this.http.delete(`http://localhost:8080/hr/api/leave-quotas/${q.id}`).subscribe({next:()=>this.loadQuotas(),error:()=>this.error.set('Suppression du quota impossible.')});}
  protected loadQuotas(){if(!this.quotaEmployee)return;this.http.get<Quota[]>(`http://localhost:8080/hr/api/employees/${this.quotaEmployee}/leave-quotas`).subscribe({next:q=>this.quotas.set(q),error:()=>this.error.set('Impossible de charger les quotas.')});}
  protected load(){
    if(this.employeeId.trim()&&!/^[0-9]{1,19}$/.test(this.employeeId.trim())){this.error.set('Identifiant employé invalide.');return;}
    const from=this.date(this.month),to=this.date(new Date(this.month.getFullYear(),this.month.getMonth()+1,0));this.loading.set(true);this.error.set('');
    forkJoin({rows:this.service.list(from,to,this.employeeId),employees:this.employeesService.getEmployees(),departments:this.departmentsService.getDepartments()}).subscribe({
      next:r=>{this.entries.set(r.rows);this.employeeDirectory.set(r.employees);this.departments.set(r.departments);this.loading.set(false);},
      error:(e:HttpErrorResponse)=>{this.entries.set([]);this.loading.set(false);this.error.set(e.status===403?'Accès réservé à l’administration.':e.status===401?'Session expirée.':'Impossible de charger le calendrier ou les annuaires Employé/Département.');}
    });
  }
  private date(v:Date){return [v.getFullYear(),String(v.getMonth()+1).padStart(2,'0'),String(v.getDate()).padStart(2,'0')].join('-');}
}
