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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Subject, catchError, finalize, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { CustomerCsvImportReport, CustomerPageParams, CustomerResponse, CustomerSort } from './customer.models';
import { CustomerService } from './customer.service';

function customerPaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Clients par page :';
  labels.nextPageLabel = 'Page suivante'; labels.previousPageLabel = 'Page précédente';
  labels.firstPageLabel = 'Première page'; labels.lastPageLabel = 'Dernière page';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-customers-page', standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSelectModule, MatSortModule, MatTableModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: customerPaginator }],
  templateUrl: './customers-page.component.html', styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomersPageComponent implements OnInit {
  private readonly service = inject(CustomerService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subject<CustomerPageParams>();
  private revision = 0;
  protected readonly rows = signal<CustomerResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly importError = signal('');
  protected readonly importReport = signal<CustomerCsvImportReport | null>(null);
  protected readonly importing = signal(false);
  protected readonly exporting = signal(false);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sortBy = signal<CustomerSort>('name');
  protected readonly direction = signal<'asc' | 'desc'>('asc');
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly contactType = new FormControl('', { nonNullable: true });
  protected readonly category = new FormControl('', { nonNullable: true });
  protected readonly segment = new FormControl('', { nonNullable: true });
  protected readonly columns = ['name', 'company', 'email', 'phone', 'actions'];

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true); this.error.set('');
      return this.service.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => { this.rows.set([]); this.total.set(0); this.error.set('Impossible de charger les clients. Réessayez.'); return EMPTY; }),
        finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
    this.load();
  }
  protected canManage() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected importCsv(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.importing()) return;
    this.importError.set('');
    this.importReport.set(null);
    if (!file.name.toLowerCase().endsWith('.csv') || file.size === 0 || file.size > 5 * 1024 * 1024) {
      this.importError.set('Sélectionnez un fichier CSV non vide de 5 Mio maximum.');
      return;
    }
    this.importing.set(true);
    this.service.importCsv(file).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.importing.set(false))).subscribe({
      next: report => {
        this.importReport.set(report);
        this.snack.open(`${report.acceptedRows} contact(s) importé(s), ${report.rejectedRows} ligne(s) rejetée(s).`, 'Fermer', { duration: 6000 });
        if (report.acceptedRows > 0) this.load();
      },
      error: (e: { status?: number }) => this.importError.set(e.status === 413
        ? 'Le fichier dépasse la limite de 5 Mio.'
        : e.status === 400 ? 'Fichier CSV invalide. Vérifiez son encodage UTF-8, ses colonnes et son format.'
          : 'Import impossible. Vérifiez votre connexion et réessayez.')
    });
  }
  protected exportCsv() {
    if (this.exporting()) return;
    const keyword = this.search.value.trim();
    this.exporting.set(true);
    this.service.exportCsv({
      ...(keyword ? { keyword } : {}),
      ...(this.contactType.value ? { contactType: this.contactType.value } : {}),
      ...(this.category.value.trim() ? { category: this.category.value.trim() } : {}),
      ...(this.segment.value.trim() ? { segment: this.segment.value.trim() } : {})
    }).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.exporting.set(false))).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'contacts-crm.csv';
        link.click();
        URL.revokeObjectURL(url);
        this.snack.open('Export CSV téléchargé.', 'Fermer', { duration: 4000 });
      },
      error: () => this.importError.set('Export impossible. Vérifiez votre connexion et réessayez.')
    });
  }
  protected exportExcel() {
    if (this.exporting()) return;
    const keyword = this.search.value.trim();
    this.exporting.set(true);
    this.service.exportExcel({
      ...(keyword ? { keyword } : {}),
      ...(this.contactType.value ? { contactType: this.contactType.value } : {}),
      ...(this.category.value.trim() ? { category: this.category.value.trim() } : {}),
      ...(this.segment.value.trim() ? { segment: this.segment.value.trim() } : {})
    }).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.exporting.set(false))).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'contacts-crm.xlsx';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.importError.set('Export Excel impossible. VÃ©rifiez votre connexion et rÃ©essayez.')
    });
  }
  protected load() {
    this.revision++;
    const keyword = this.search.value.trim();
    this.requests.next({ page: this.page(), size: this.size(), sortBy: this.sortBy(),
      direction: this.direction(), ...(keyword ? { keyword } : {}),
      ...(this.contactType.value ? { contactType: this.contactType.value } : {}),
      ...(this.category.value.trim() ? { category: this.category.value.trim() } : {}),
      ...(this.segment.value.trim() ? { segment: this.segment.value.trim() } : {}) });
  }
  protected find() { this.page.set(0); this.load(); }
  protected paginate(event: PageEvent) { this.page.set(event.pageIndex); this.size.set(event.pageSize); this.load(); }
  protected convert(row: CustomerResponse) {
    if (!this.canManage() || row.contactType !== 'LEAD' || this.busy()) return;
    this.busy.set(true);
    this.service.convertLead(row.id).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.snack.open('Prospect converti en client.', 'Fermer', { duration: 4000 }); this.load(); },
      error: (e: { status?: number }) => this.snack.open(e.status === 400 ? 'Seuls les prospects peuvent être convertis.' : 'Conversion impossible. Réessayez.', 'Fermer', { duration: 7000 })
    });
  }
  protected sort(event: Sort) {
    const fields: CustomerSort[] = ['id', 'name', 'company', 'email', 'phone'];
    this.sortBy.set(fields.includes(event.active as CustomerSort) ? event.active as CustomerSort : 'name');
    this.direction.set(event.direction === 'desc' ? 'desc' : 'asc'); this.page.set(0); this.load();
  }
  protected remove(customer: CustomerResponse) {
    if (!this.canManage() || this.busy() || this.loading()) return;
    const revision = this.revision;
    const keyword = this.search.value.trim();
    const last = this.rows().length === 1 && this.rows()[0].id === customer.id;
    this.busy.set(true);
    this.dialog.open(ConfirmDialogComponent, { width: '440px', maxWidth: '90vw',
      data: { title: 'Supprimer le client', message: `Supprimer « ${customer.name} » ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer' }
    }).afterClosed().pipe(switchMap(confirmed => confirmed === true && this.canManage()
      ? this.service.delete(customer.id) : EMPTY),
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.snack.open('Client supprimé.', 'Fermer', { duration: 5000 });
        if (last && revision === this.revision && keyword === this.search.value.trim() && this.page() > 0)
          this.page.update(page => page - 1);
        this.load();
      },
      error: (e: { status?: number }) => {
        if (e.status === 401 || e.status === 403) return;
        this.snack.open(e.status === 404 ? 'Ce client est introuvable ou a déjà été supprimé.'
          : e.status === 400 || e.status === 409 ? 'Suppression refusée par le service. Le client peut être lié à d’autres données.'
          : 'Impossible de supprimer ce client. Réessayez plus tard.', 'Fermer', { duration: 8000 });
      }
    });
  }
}
