import { PdfDownloadComponent } from '../../shared/pdf-download/pdf-download.component';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, finalize, switchMap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { LeaveResponse } from './leave.models';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leaves-page', standalone: true,
  imports: [DatePipe, MatFormFieldModule, MatInputModule, MatPaginatorModule, PdfDownloadComponent, RouterLink, MatButtonModule, MatCardModule, MatTableModule],
  templateUrl: './leaves-page.component.html', changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeavesPageComponent implements OnInit {
  private readonly service = inject(LeaveService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly leaves = signal<LeaveResponse[]>([]);
  protected readonly query = signal('');
  protected readonly statusFilter = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly selected = signal<LeaveResponse|null>(null);
  protected readonly statusLabels: Record<string,string> = {PENDING:'En attente',APPROVED:'Approuvée',REJECTED:'Refusée'};
  protected readonly filtered = computed(() => {
    const query=this.query().trim().toLocaleLowerCase('fr');
    return this.leaves().filter(row=>(!query||[String(row.id),row.employeeId,row.reason,row.startDate,row.endDate]
      .some(value=>value?.toLocaleLowerCase('fr').includes(query)))&&(!this.statusFilter()||row.status===this.statusFilter()));
  });
  protected readonly visible = computed(()=>this.filtered().slice(this.pageIndex()*this.pageSize(),(this.pageIndex()+1)*this.pageSize()));
  protected filter(query:string,status:string){this.query.set(query);this.statusFilter.set(status);this.pageIndex.set(0);}
  protected paginate(event:PageEvent){this.pageIndex.set(event.pageIndex);this.pageSize.set(event.pageSize);}
  private generation=this.auth.sessionGeneration;
  constructor(){
    effect(()=>{this.auth.getCurrentUser();const generation=this.auth.sessionGeneration;const allowed=this.isAdmin()||this.isEmployee();
      untracked(()=>{if(generation!==this.generation||!allowed){
        this.generation=generation;this.leaves.set([]);this.selected.set(null);this.filter('','');
        this.loading.set(false);this.busy.set(false);this.error.set('');this.notice.set('');if(allowed)this.load();
      }});
    });
  }
  private current(generation:number){return generation===this.auth.sessionGeneration&&!this.destroyRef.destroyed&&(this.isAdmin()||this.isEmployee());}
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly columns = ['employee', 'start', 'end', 'reason', 'status', 'validator', 'actions'];
  protected isAdmin() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected isEmployee() { return !this.isAdmin() && this.auth.hasAnyRole([CrmRole.EMPLOYEE]); }
  ngOnInit() { this.load(); }
  protected load() {
    if (this.loading() || (!this.isAdmin()&&!this.isEmployee())) return;
    const generation=this.auth.sessionGeneration;
    this.loading.set(true); this.error.set('');
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {if(this.current(generation))this.loading.set(false);})).subscribe({
      next: rows => {if(this.current(generation)){this.leaves.set(rows);this.pageIndex.set(0);this.selected.set(null);}},
      error: (e: { status?: number }) => { if(!this.current(generation))return;this.leaves.set([]);this.selected.set(null); this.error.set(e.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' : e.status === 403
        ? 'Accès refusé. Pour un compte employé, demandez à un administrateur de vérifier votre association Employee pour les congés.'
        : 'Impossible de charger les demandes. Réessayez.'); }
    });
  }
  protected act(row: LeaveResponse, action: 'approve' | 'reject' | 'delete') {
    if (this.busy() || this.loading() || row.status !== 'PENDING') return;
    if (action === 'delete' ? !(this.isEmployee()||this.isAdmin()) : !this.isAdmin()) return;
    const generation=this.auth.sessionGeneration;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    const label = action === 'approve' ? 'Approuver' : action === 'reject' ? 'Refuser' : 'Supprimer';
    this.dialog.open(ConfirmDialogComponent, { data: { title: `${label} la demande`, message: `${label} cette demande de congé ?`, confirmLabel: label } }).afterClosed().pipe(
      switchMap(yes => {
        if (yes !== true || !this.current(generation) || (action === 'delete' ? !(this.isEmployee()||this.isAdmin()) : !this.isAdmin())) return EMPTY;
        return action === 'approve' ? this.service.approve(row.id) : action === 'reject' ? this.service.reject(row.id) : this.service.delete(row.id);
      }), takeUntilDestroyed(this.destroyRef), finalize(() => {if(this.current(generation))this.busy.set(false);})).subscribe({
      next: () => {if(this.current(generation)){this.notice.set('Demande mise à jour.');this.load();}},
      error: (e: { status?: number }) => {if(this.current(generation))this.error.set(e.status === 400 || e.status === 409
        ? 'Cette demande a déjà été décidée ou modifiée. Rechargez la liste.' : 'Action refusée ou service indisponible. Réessayez.');}
    });
  }
}
