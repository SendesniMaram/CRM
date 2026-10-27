import { DatePipe } from '@angular/common';
import { InvoiceAmountPipe } from '../invoices/invoice-amount.pipe';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Subject, catchError, finalize, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { PayrollPageParams, PayrollResponse, PayrollSort } from './payroll.models';
import { PayrollService } from './payroll.service';

function payrollPaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Fiches par page :';
  labels.nextPageLabel = 'Page suivante'; labels.previousPageLabel = 'Page précédente';
  labels.firstPageLabel = 'Première page'; labels.lastPageLabel = 'Dernière page';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-payrolls-page', standalone: true,
  imports: [DatePipe, InvoiceAmountPipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSortModule, MatTableModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: payrollPaginator }],
  templateUrl: './payrolls-page.component.html', styleUrl: './payroll.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PayrollsPageComponent implements OnInit {
  private readonly service = inject(PayrollService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subject<PayrollPageParams>();
  private revision = 0;
  protected readonly rows = signal<PayrollResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sortBy = signal<PayrollSort>('payPeriod');
  protected readonly direction = signal<'asc' | 'desc'>('asc');
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly columns = ['payPeriod', 'employeeId', 'paymentDate', 'netSalary', 'currency', 'status', 'published', 'actions'];

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true); this.error.set('');
      return this.service.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => { this.rows.set([]); this.total.set(0); this.error.set('Impossible de charger les fiches de paie. Réessayez.'); return EMPTY; }),
        finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
    this.load();
  }
  protected canManage() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected load() {
    this.revision++;
    const keyword = this.search.value.trim();
    this.requests.next({ page: this.page(), size: this.size(), sortBy: this.sortBy(),
      direction: this.direction(), ...(keyword ? { keyword } : {}) });
  }
  protected find() { this.page.set(0); this.load(); }
  protected paginate(event: PageEvent) { this.page.set(event.pageIndex); this.size.set(event.pageSize); this.load(); }
  protected sort(event: Sort) {
    const fields: PayrollSort[] = ['id', 'payPeriod', 'employeeId', 'paymentDate', 'netSalary', 'currency', 'status'];
    this.sortBy.set(fields.includes(event.active as PayrollSort) ? event.active as PayrollSort : 'payPeriod');
    this.direction.set(event.direction === 'desc' ? 'desc' : 'asc'); this.page.set(0); this.load();
  }
  protected remove(payroll: PayrollResponse) {
    if (!this.canManage() || this.busy() || this.loading()) return;
    const revision = this.revision;
    const keyword = this.search.value.trim();
    const last = this.rows().length === 1 && this.rows()[0].id === payroll.id;
    this.busy.set(true);
    this.dialog.open(ConfirmDialogComponent, { width: '440px', maxWidth: '90vw',
      data: { title: 'Supprimer la fiche de paie', message: `Supprimer la fiche « ${payroll.payPeriod} » de l’employé ${payroll.employeeId} ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer' }
    }).afterClosed().pipe(switchMap(confirmed => confirmed === true && this.canManage()
      ? this.service.delete(payroll.id) : EMPTY),
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.snack.open('Fiche de paie supprimée.', 'Fermer', { duration: 5000 });
        if (last && revision === this.revision && keyword === this.search.value.trim() && this.page() > 0)
          this.page.update(page => page - 1);
        this.load();
      },
      error: (e: { status?: number }) => {
        if (e.status === 401 || e.status === 403) return;
        this.snack.open(e.status === 404 ? 'Cette fiche de paie est introuvable ou a déjà été supprimée.'
          : e.status === 400 || e.status === 409 ? 'Suppression refusée par le service. La fiche peut être liée à d’autres données.'
          : 'Impossible de supprimer cette fiche de paie. Réessayez plus tard.', 'Fermer', { duration: 8000 });
      }
    });
  }

  protected togglePublication(payroll: PayrollResponse) {
    if (!this.canManage() || this.busy() || this.loading()) return;
    this.busy.set(true); this.error.set('');
    this.service.setPublished(payroll.id, !payroll.published).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))
    ).subscribe({
      next: result => {
        this.snack.open(result.published ? 'Bulletin publié pour l’employé.' : 'Publication retirée.', 'Fermer', { duration: 5000 });
        this.load();
      },
      error: (e: { status?: number }) => {
        if (e.status === 401 || e.status === 403) return;
        this.snack.open(e.status === 404 ? 'Cette fiche de paie est introuvable.'
          : 'Impossible de modifier la publication du bulletin.', 'Fermer', { duration: 8000 });
      }
    });
  }
}
