import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, finalize, forkJoin, switchMap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { LeaveAssociation, LeaveDirectoryOptions, LeaveResponse } from './leave.models';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leaves-page', standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatSelectModule, MatTableModule],
  templateUrl: './leaves-page.component.html', changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeavesPageComponent implements OnInit {
  private readonly service = inject(LeaveService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  protected readonly leaves = signal<LeaveResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly directory = signal<LeaveDirectoryOptions | null>(null);
  protected readonly links = signal<LeaveAssociation[]>([]);
  protected readonly directoryLoading = signal(false);
  protected readonly directoryError = signal('');
  protected readonly columns = ['employee', 'start', 'end', 'reason', 'status', 'validator', 'actions'];
  protected readonly linkForm = this.fb.group({ userId: [null as number | null, Validators.required], employeeId: [null as number | null, Validators.required] });
  protected isAdmin() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected isEmployee() { return !this.isAdmin() && this.auth.hasAnyRole([CrmRole.EMPLOYEE]); }
  ngOnInit() { this.load(); }
  protected load() {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: rows => this.leaves.set(rows),
      error: (e: { status?: number }) => { this.leaves.set([]); this.error.set(e.status === 403
        ? 'Accès refusé. Pour un compte employé, demandez à un administrateur de vérifier votre association Employee pour les congés.'
        : 'Impossible de charger les demandes. Réessayez.'); }
    });
  }
  protected loadDirectory() {
    if (!this.isAdmin() || this.directoryLoading()) return;
    this.directoryLoading.set(true); this.directoryError.set('');
    forkJoin({ options: this.service.options(), links: this.service.associations() }).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.directoryLoading.set(false))).subscribe({
      next: result => { this.directory.set(result.options); this.links.set(result.links); },
      error: () => { this.directory.set(null); this.directoryError.set('Impossible de charger les comptes Employee et les employés. Vérifiez Identity et Employee puis réessayez.'); }
    });
  }
  protected associate() {
    if (!this.isAdmin() || this.busy() || this.linkForm.invalid) return;
    const { userId, employeeId } = this.linkForm.getRawValue();
    if (userId === null || employeeId === null) return;
    this.busy.set(true); this.directoryError.set('');
    this.service.associate(userId, employeeId).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.linkForm.reset(); this.notice.set('Association enregistrée.'); this.loadDirectory(); },
      error: () => this.directoryError.set('Association refusée. Choisissez un compte EMPLOYEE et un employé existants, sans association différente déjà enregistrée.')
    });
  }
  protected act(row: LeaveResponse, action: 'approve' | 'reject' | 'delete') {
    if (this.busy() || this.loading() || row.status !== 'PENDING') return;
    if (action === 'delete' ? !this.isEmployee() : !this.isAdmin()) return;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    const label = action === 'approve' ? 'Approuver' : action === 'reject' ? 'Refuser' : 'Supprimer';
    this.dialog.open(ConfirmDialogComponent, { data: { title: `${label} la demande`, message: `${label} cette demande de congé ?`, confirmLabel: label } }).afterClosed().pipe(
      switchMap(yes => {
        if (yes !== true || (action === 'delete' ? !this.isEmployee() : !this.isAdmin())) return EMPTY;
        return action === 'approve' ? this.service.approve(row.id) : action === 'reject' ? this.service.reject(row.id) : this.service.delete(row.id);
      }), takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.notice.set('Demande mise à jour.'); this.load(); },
      error: (e: { status?: number }) => this.error.set(e.status === 400 || e.status === 409
        ? 'Cette demande a déjà été décidée ou modifiée. Rechargez la liste.' : 'Action refusée ou service indisponible. Réessayez.')
    });
  }
}
