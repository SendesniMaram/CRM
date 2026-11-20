import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, finalize, switchMap, tap } from 'rxjs';
import { CustomerResponse } from '../customers/customer.models';
import { CustomerService } from '../customers/customer.service';
import { QuoteRequest, QuoteResponse, QuoteStatus } from './quote.models';
import { QuoteQuery, QuoteService } from './quote.service';

const STATUSES: readonly QuoteStatus[] = ['DRAFT', 'SENT', 'ACCEPTED', 'REJECTED'];

function quotePaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Devis par page :';
  labels.nextPageLabel = 'Page suivante';
  labels.previousPageLabel = 'Page précédente';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-quotes-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSelectModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: quotePaginator }],
  templateUrl: './quotes-page.component.html',
  styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuotesPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(QuoteService);
  private readonly customersApi = inject(CustomerService);
  private readonly destroy = inject(DestroyRef);
  private readonly snack = inject(MatSnackBar);
  private readonly requests = new Subject<QuoteQuery>();

  protected readonly statuses = STATUSES;
  protected readonly rows = signal<QuoteResponse[]>([]);
  protected readonly customers = signal<CustomerResponse[]>([]);
  protected readonly selectedId = signal<number | null>(null);
  protected readonly detail = signal<QuoteResponse | null>(null);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly search = this.fb.nonNullable.control('');
  protected readonly statusFilter = this.fb.nonNullable.control<QuoteStatus | ''>('');
  protected readonly customerSearch = this.fb.nonNullable.control('');
  protected readonly form = this.fb.nonNullable.group({
    customerId: ['', Validators.required],
    issueDate: [''],
    expiryDate: [''],
    lines: this.fb.array([this.createLine()]),
    tax: [0, [Validators.required, Validators.min(0), Validators.max(9999999999)]],
    currency: ['', Validators.maxLength(10)],
    notes: ['', Validators.maxLength(500)]
  });

  get lineControls(): FormArray { return this.form.controls.lines; }

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true);
      this.error.set('');
      return this.api.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => {
          this.rows.set([]);
          this.total.set(0);
          this.error.set('Impossible de charger les devis.');
          return EMPTY;
        }),
        finalize(() => this.loading.set(false))
      );
    }), takeUntilDestroyed(this.destroy)).subscribe();
    this.load();
    this.loadCustomers();
  }

  protected statusLabel(status: QuoteStatus) {
    return { DRAFT: 'Brouillon', SENT: 'Envoyé', ACCEPTED: 'Accepté', REJECTED: 'Refusé' }[status];
  }

  protected find() { this.page.set(0); this.load(); }
  protected paginate(event: PageEvent) { this.page.set(event.pageIndex); this.size.set(event.pageSize); this.load(); }
  protected addLine() { this.lineControls.push(this.createLine()); }
  protected removeLine(index: number) {
    if (this.lineControls.length > 1) this.lineControls.removeAt(index);
  }

  protected customerName(id: string) {
    return this.customers().find(customer => String(customer.id) === id)?.name ?? `Client #${id}`;
  }

  protected totalForDraft() {
    const raw = this.form.getRawValue();
    const subtotalCents = raw.lines.reduce((sum, line) =>
      sum + Math.round((Number(line.quantity) || 0) * (Number(line.unitPrice) || 0) * 100), 0);
    const totalCents = subtotalCents + Math.round((Number(raw.tax) || 0) * 100);
    return (totalCents / 100).toFixed(2);
  }

  protected view(quote: QuoteResponse) {
    this.detail.set(null);
    this.api.get(quote.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: row => this.detail.set(row),
      error: () => this.error.set('Impossible de charger le détail du devis.')
    });
  }

  protected edit(quote: QuoteResponse) {
    if (quote.status !== 'DRAFT' || quote.convertedInvoiceId !== null) return;
    this.selectedId.set(quote.id);
    this.error.set('');
    this.form.setControl('lines', this.fb.array(quote.lines.map(line => this.createLine(
      line.description, line.quantity, line.unitPrice
    ))));
    this.form.patchValue({
      customerId: quote.customerId,
      issueDate: quote.issueDate ?? '',
      expiryDate: quote.expiryDate ?? '',
      tax: quote.tax ?? 0,
      currency: quote.currency ?? '',
      notes: quote.notes ?? ''
    });
    if (!this.customers().some(customer => String(customer.id) === quote.customerId)) {
      this.customersApi.get(Number(quote.customerId)).pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: customer => this.customers.update(rows => [customer, ...rows]),
        error: () => this.error.set('Le client associé au devis est introuvable.')
      });
    }
  }

  protected startNew() {
    this.selectedId.set(null);
    this.detail.set(null);
    this.form.reset({ customerId: '', issueDate: '', expiryDate: '', tax: 0, currency: '', notes: '' });
    this.form.setControl('lines', this.fb.array([this.createLine()]));
    this.error.set('');
  }

  protected save() {
    if (this.saving()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const request: QuoteRequest = {
      customerId: value.customerId,
      issueDate: value.issueDate || null,
      expiryDate: value.expiryDate || null,
      lines: value.lines.map(line => ({
        description: line.description.trim(),
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice)
      })),
      tax: Number(value.tax),
      currency: value.currency.trim() || null,
      status: 'DRAFT',
      notes: value.notes.trim() || null
    };
    const existingId = this.selectedId();
    this.saving.set(true);
    this.error.set('');
    (existingId === null ? this.api.create(request) : this.api.update(existingId, request)).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.saving.set(false))
    ).subscribe({
      next: quote => {
        this.snack.open(existingId === null ? 'Devis créé.' : 'Devis mis à jour.', 'Fermer', { duration: 4000 });
        this.startNew();
        this.load();
      },
      error: (e: { status?: number }) => {
        this.error.set(e.status === 409
          ? 'Seuls les devis brouillons non convertis peuvent être modifiés.'
          : e.status === 400
            ? 'Vérifiez les dates, le client, le montant de taxe et les lignes du devis.'
            : 'Enregistrement du devis impossible.');
      }
    });
  }

  protected transition(quote: QuoteResponse, status: QuoteStatus) {
    if (this.busy() || quote.convertedInvoiceId !== null) return;
    this.busy.set(true);
    this.api.status(quote.id, status).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: updated => this.rows.update(rows => rows.map(row => row.id === updated.id ? updated : row)),
      error: () => this.error.set('Impossible de changer le statut définitif du devis.')
    });
  }

  protected convert(quote: QuoteResponse) {
    if (this.busy() || quote.status !== 'ACCEPTED' || quote.convertedInvoiceId !== null) return;
    this.busy.set(true);
    this.api.convert(quote.id).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: result => {
        this.rows.update(rows => rows.map(row => row.id === result.quote.id ? result.quote : row));
        this.snack.open(`Facture ${result.invoice.invoiceNumber} créée.`, 'Fermer', { duration: 5000 });
      },
      error: (e: { status?: number }) => this.error.set(e.status === 409
        ? 'Seul un devis accepté peut être converti.'
        : 'Conversion impossible. La facture n’a pas été confirmée.')
    });
  }

  protected download(quote: QuoteResponse) {
    this.api.pdf(quote.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${quote.quoteNumber}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('PDF du devis indisponible.')
    });
  }

  protected searchCustomers() { this.loadCustomers(this.customerSearch.value.trim()); }

  private load() {
    this.requests.next({
      page: this.page(),
      size: this.size(),
      ...(this.search.value.trim() ? { keyword: this.search.value.trim() } : {}),
      ...(this.statusFilter.value ? { status: this.statusFilter.value } : {})
    });
  }

  private loadCustomers(keyword = '') {
    this.customersApi.list({
      page: 0,
      size: 25,
      sortBy: 'name',
      direction: 'asc',
      ...(keyword ? { keyword } : {})
    }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: result => this.customers.set(result.content.filter(customer => customer.contactType !== 'SUPPLIER')),
      error: () => this.error.set('Répertoire des clients indisponible.')
    });
  }

  private createLine(description = '', quantity = 1, unitPrice = 0) {
    return this.fb.nonNullable.group({
      description: [description, [Validators.required, Validators.maxLength(255)]],
      quantity: [quantity, [Validators.required, Validators.min(0.001), Validators.max(999999999),
        Validators.pattern(/^\d+(\.\d{1,3})?$/)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0), Validators.max(9999999999),
        Validators.pattern(/^\d+(\.\d{1,2})?$/)]]
    });
  }
}
