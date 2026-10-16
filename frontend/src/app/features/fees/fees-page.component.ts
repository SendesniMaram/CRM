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
import { FeePageParams, FeeResponse, FeeSort } from './fee.models';
import { FeeService } from './fee.service';

function feePaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Honoraires par page :';
  labels.nextPageLabel = 'Page suivante'; labels.previousPageLabel = 'Page précédente';
  labels.firstPageLabel = 'Première page'; labels.lastPageLabel = 'Dernière page';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-fees-page', standalone: true,
  imports: [DatePipe, InvoiceAmountPipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSortModule, MatTableModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: feePaginator }],
  templateUrl: './fees-page.component.html', styleUrl: './fees.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeesPageComponent implements OnInit {
  private readonly service = inject(FeeService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subject<FeePageParams>();
  private revision = 0;
  protected readonly rows = signal<FeeResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sortBy = signal<FeeSort>('serviceName');
  protected readonly direction = signal<'asc' | 'desc'>('asc');
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly columns = ['serviceName', 'employeeId', 'customerId', 'workDate', 'amount', 'invoiceStatus', 'paymentStatus', 'actions'];

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true); this.error.set('');
      return this.service.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => { this.rows.set([]); this.total.set(0); this.error.set('Impossible de charger les honoraires. Réessayez.'); return EMPTY; }),
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
    const fields: FeeSort[] = ['id', 'serviceName', 'employeeId', 'customerId', 'workDate', 'amount', 'invoiceStatus', 'paymentStatus'];
    this.sortBy.set(fields.includes(event.active as FeeSort) ? event.active as FeeSort : 'serviceName');
    this.direction.set(event.direction === 'desc' ? 'desc' : 'asc'); this.page.set(0); this.load();
  }
  protected remove(fee: FeeResponse) {
    if (!this.canManage() || this.busy() || this.loading()) return;
    const revision = this.revision;
    const keyword = this.search.value.trim();
    const last = this.rows().length === 1 && this.rows()[0].id === fee.id;
    this.busy.set(true);
    this.dialog.open(ConfirmDialogComponent, { width: '440px', maxWidth: '90vw',
      data: { title: 'Supprimer l’honoraire', message: `Supprimer « ${fee.serviceName} » ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer' }
    }).afterClosed().pipe(switchMap(confirmed => confirmed === true && this.canManage()
      ? this.service.delete(fee.id) : EMPTY),
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.snack.open('Honoraire supprimé.', 'Fermer', { duration: 5000 });
        if (last && revision === this.revision && keyword === this.search.value.trim() && this.page() > 0)
          this.page.update(page => page - 1);
        this.load();
      },
      error: (e: { status?: number }) => {
        if (e.status === 401 || e.status === 403) return;
        this.snack.open(e.status === 404 ? 'Cet honoraire est introuvable ou a déjà été supprimé.'
          : e.status === 400 || e.status === 409 ? 'Suppression refusée par le service. L’honoraire peut être lié à d’autres données.'
          : 'Impossible de supprimer cet honoraire. Réessayez plus tard.', 'Fermer', { duration: 8000 });
      }
    });
  }
}
