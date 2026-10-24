import { DatePipe } from '@angular/common';
import { InvoiceAmountPipe } from './invoice-amount.pipe';
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
import { InvoicePageParams, InvoiceResponse, InvoiceSort } from './invoice.models';
import { InvoiceService } from './invoice.service';

function invoicePaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Factures par page :';
  labels.nextPageLabel = 'Page suivante'; labels.previousPageLabel = 'Page précédente';
  labels.firstPageLabel = 'Première page'; labels.lastPageLabel = 'Dernière page';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-invoices-page', standalone: true,
  imports: [DatePipe, InvoiceAmountPipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSortModule, MatTableModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: invoicePaginator }],
  templateUrl: './invoices-page.component.html', styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoicesPageComponent implements OnInit {
  private readonly service = inject(InvoiceService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subject<InvoicePageParams>();
  private revision = 0;
  protected readonly rows = signal<InvoiceResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sortBy = signal<InvoiceSort>('invoiceNumber');
  protected readonly direction = signal<'asc' | 'desc'>('asc');
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly columns = ['invoiceNumber', 'customerId', 'issueDate', 'totalAmount', 'status', 'actions'];

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true); this.error.set('');
      return this.service.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => { this.rows.set([]); this.total.set(0); this.error.set('Impossible de charger les factures. Réessayez.'); return EMPTY; }),
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
    const fields: InvoiceSort[] = ['id', 'invoiceNumber', 'customerId', 'issueDate', 'totalAmount', 'status'];
    this.sortBy.set(fields.includes(event.active as InvoiceSort) ? event.active as InvoiceSort : 'invoiceNumber');
    this.direction.set(event.direction === 'desc' ? 'desc' : 'asc'); this.page.set(0); this.load();
  }
  protected remove(invoice: InvoiceResponse) {
    if (!this.canManage() || this.busy() || this.loading()) return;
    const revision = this.revision;
    const keyword = this.search.value.trim();
    const last = this.rows().length === 1 && this.rows()[0].id === invoice.id;
    this.busy.set(true);
    this.dialog.open(ConfirmDialogComponent, { width: '440px', maxWidth: '90vw',
      data: { title: 'Supprimer la facture', message: `Supprimer « ${invoice.invoiceNumber} » ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer' }
    }).afterClosed().pipe(switchMap(confirmed => confirmed === true && this.canManage()
      ? this.service.delete(invoice.id) : EMPTY),
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.snack.open('Facture supprimée.', 'Fermer', { duration: 5000 });
        if (last && revision === this.revision && keyword === this.search.value.trim() && this.page() > 0)
          this.page.update(page => page - 1);
        this.load();
      },
      error: (e: { status?: number }) => {
        if (e.status === 401 || e.status === 403) return;
        this.snack.open(e.status === 404 ? 'Cette facture est introuvable ou a déjà été supprimée.'
          : e.status === 400 || e.status === 409 ? 'Suppression refusée par le service. La facture peut être liée à d’autres données.'
          : 'Impossible de supprimer cette facture. Réessayez plus tard.', 'Fermer', { duration: 8000 });
      }
    });
  }
}
