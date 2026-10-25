import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, finalize, switchMap, tap } from 'rxjs';
import { CustomerResponse } from '../customers/customer.models';
import { CustomerService } from '../customers/customer.service';
import { InvoiceRequest, parseCustomerId } from './invoice.models';
import { InvoiceService } from './invoice.service';

const notBlank = (control: AbstractControl) => typeof control.value === 'string' && control.value.trim() ? null : { required: true };
const finite = (control: AbstractControl) => control.value === null || Number.isFinite(control.value) ? null : { number: true };
export function invoiceDate(control: AbstractControl) {
  const value: string = control.value;
  if (!value) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return { date: true };
  const date = new Date(value + 'T00:00:00Z');
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value ? null : { date: true };
}

@Component({
  selector: 'app-invoice-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule],
  templateUrl: './invoice-form.component.html', styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(InvoiceService);
  private readonly customers = inject(CustomerService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  private readonly customerLoads = new Subject<void>();
  protected readonly id = signal<number | null>(null);
  protected readonly ready = signal(false);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly choices = signal<CustomerResponse[]>([]);
  protected readonly selected = signal<CustomerResponse | null>(null);
  protected readonly customerLoading = signal(false);
  protected readonly customerError = signal('');
  protected readonly customerNotice = signal('');
  protected readonly customerPage = signal(0);
  protected readonly customerPages = signal(0);
  protected readonly customerTotal = signal(0);
  protected readonly customerSearch = new FormControl('', { nonNullable: true });
  protected readonly form = this.fb.group({
    invoiceNumber: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)]),
    customerId: this.fb.control<number | null>(null, [Validators.required, control =>
      Number.isSafeInteger(control.value) && control.value > 0 &&
        (this.selected()?.id === control.value || this.choices().some(c => c.id === control.value))
        ? null : { customer: true }]),
    issueDate: this.fb.nonNullable.control('', invoiceDate),
    dueDate: this.fb.nonNullable.control('', invoiceDate),
    subtotal: this.fb.control<number | null>(null, [Validators.required, finite, control =>
      control.value === null || control.value > 0 ? null : { positive: true }]),
    tax: this.fb.control<number | null>(null, finite),
    currency: this.fb.nonNullable.control('', Validators.maxLength(10)),
    status: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)]),
    notes: this.fb.nonNullable.control('', Validators.maxLength(500))
  });

  ngOnInit() {
    this.customerLoads.pipe(switchMap(() => {
      this.customerLoading.set(true); this.customerError.set('');
      return this.customers.list({ page: this.customerPage(), size: 25, sortBy: 'name', direction: 'asc',
        keyword: this.customerSearch.value.trim() }).pipe(tap(page => {
        this.choices.set(page.content); this.customerPages.set(page.totalPages); this.customerTotal.set(page.totalElements);
        this.form.controls.customerId.updateValueAndValidity();
      }), catchError(() => {
        this.choices.set([]); this.customerError.set('Impossible de charger les clients. Réessayez.');
        this.form.controls.customerId.updateValueAndValidity(); return EMPTY;
      }), finalize(() => this.customerLoading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
    this.loadCustomers();
    this.route.paramMap.pipe(switchMap(params => {
      this.ready.set(false); this.error.set(''); this.customerNotice.set(''); this.selected.set(null);
      this.form.reset(); this.id.set(null); this.loading.set(false);
      const raw = params.get('id');
      if (raw === null) { this.ready.set(true); return EMPTY; }
      const id = Number(raw);
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Facture introuvable.'); return EMPTY; }
      this.id.set(id); this.loading.set(true);
      return this.service.get(id).pipe(switchMap(row => {
        this.form.patchValue({ invoiceNumber: row.invoiceNumber, issueDate: row.issueDate ?? '',
          dueDate: row.dueDate ?? '', subtotal: row.subtotal, tax: row.tax, currency: row.currency ?? '',
          status: row.status, notes: row.notes ?? '' });
        this.ready.set(true);
        const clientId = parseCustomerId(row.customerId);
        if (clientId === null) {
          this.customerNotice.set('La référence client enregistrée ne correspond pas à un identifiant sélectionnable. Choisissez un client existant.');
          return EMPTY;
        }
        return this.customers.get(clientId).pipe(tap(client => {
          if (client.id !== clientId) { this.customerNotice.set('Référence client incohérente. Choisissez un client existant.'); return; }
          this.selected.set(client); this.form.controls.customerId.setValue(clientId);
        }), catchError(() => {
          this.customerNotice.set('Le client associé est introuvable ou indisponible. Choisissez un client existant.'); return EMPTY;
        }));
      }), catchError((e: { status?: number }) => {
        this.ready.set(false); this.error.set(e.status === 404 ? 'Facture introuvable.' : 'Impossible de charger la facture. Réessayez.');
        return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected loadCustomers() { this.customerLoads.next(); }
  protected searchCustomers() { this.customerPage.set(0); this.loadCustomers(); }
  protected changeCustomerPage(offset: number) {
    const page = this.customerPage() + offset;
    if (page >= 0 && page < this.customerPages()) { this.customerPage.set(page); this.loadCustomers(); }
  }
  protected pinnedCustomer() {
    const selected = this.selected();
    return selected && !this.choices().some(c => c.id === selected.id) ? selected : null;
  }
  protected selectCustomer(value: unknown) {
    const choice = this.choices().find(c => c.id === value) ?? (this.selected()?.id === value ? this.selected() : null);
    this.selected.set(choice ?? null); this.form.controls.customerId.updateValueAndValidity();
  }
  protected submit() {
    if (!this.ready() || this.saving() || this.loading() || this.customerLoading() || this.customerError()) return;
    Object.values(this.form.controls).forEach(control => control.updateValueAndValidity());
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    if (value.customerId === null || value.subtotal === null) return;
    const request: InvoiceRequest = { invoiceNumber: value.invoiceNumber.trim(), customerId: String(value.customerId),
      issueDate: value.issueDate || null, dueDate: value.dueDate || null, subtotal: value.subtotal,
      tax: value.tax, currency: value.currency.trim() || null, status: value.status.trim(), notes: value.notes.trim() || null };
    this.saving.set(true); this.error.set('');
    const id = this.id();
    (id === null ? this.service.create(request) : this.service.update(id, request)).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.snack.open(id === null ? 'Facture créée.' : 'Facture modifiée.', 'Fermer', { duration: 5000 });
        void this.router.navigate(['/invoices']);
      },
      error: (e: { status?: number; error?: { message?: unknown } }) => {
        if (e.status === 401 || e.status === 403) return;
        if (e.status === 404) { this.ready.set(false); this.error.set('Cette facture n’existe plus.'); return; }
        if (e.status === 400 || e.status === 409) {
          const message = typeof e.error?.message === 'string' ? e.error.message : '';
          for (const key of Object.keys(this.form.controls) as (keyof typeof this.form.controls)[]) {
            if (message.includes(key + ':') || (key === 'invoiceNumber' && /Invoice number already exists/i.test(message))) {
              this.form.controls[key].setErrors({ server: true }); this.form.controls[key].markAsTouched();
            }
          }
          this.error.set('Enregistrement refusé. Vérifiez les champs et l’unicité du numéro de facture.');
        } else this.error.set('Impossible d’enregistrer la facture. Réessayez plus tard.');
      }
    });
  }
}
