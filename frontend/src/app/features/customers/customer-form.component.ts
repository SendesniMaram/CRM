import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { CustomerRequest } from './customer.models';
import { CustomerService } from './customer.service';

const trimmedEmail = (control: AbstractControl) => Validators.email({ value: control.value.trim() } as AbstractControl);
const notBlank = (control: AbstractControl) => control.value.trim() ? null : { required: true };

@Component({
  selector: 'app-customer-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule],
  templateUrl: './customer-form.component.html', styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CustomerService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  protected readonly id = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly ready = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly fields = [
    { key: 'name', label: 'Nom du client', max: 255, type: 'text', autocomplete: 'name' },
    { key: 'company', label: 'Société', max: 255, type: 'text', autocomplete: 'organization' },
    { key: 'email', label: 'E-mail', max: 255, type: 'email', autocomplete: 'email' },
    { key: 'phone', label: 'Téléphone', max: 50, type: 'tel', autocomplete: 'tel' }
  ] as const;
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, notBlank, Validators.maxLength(255)]],
    company: ['', Validators.maxLength(255)],
    email: ['', [trimmedEmail, Validators.maxLength(255)]],
    phone: ['', [Validators.maxLength(50), (control: AbstractControl) => {
      const value = control.value.trim();
      return !value || /^(\+\d{1,3}[- ]?)?\d{6,14}$/.test(value) ? null : { pattern: true };
    }]],
    address: ['', Validators.maxLength(500)]
    ,contactType: ['CUSTOMER' as 'LEAD' | 'CUSTOMER' | 'SUPPLIER']
    ,category: ['', Validators.maxLength(100)]
    ,segment: ['', Validators.maxLength(100)]
  });
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.form.reset(); this.ready.set(false); this.error.set(''); this.id.set(null); this.loading.set(false);
      const raw = params.get('id');
      if (raw === null) { this.ready.set(true); return EMPTY; }
      const id = Number(raw);
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Client introuvable.'); return EMPTY; }
      this.id.set(id); this.loading.set(true);
      return this.service.get(id).pipe(tap(row => {
        this.form.patchValue({ name: row.name, company: row.company ?? '', email: row.email ?? '',
          phone: row.phone ?? '', address: row.address ?? '', contactType: row.contactType ?? 'CUSTOMER', category: row.category ?? '', segment: row.segment ?? '' }); this.ready.set(true);
      }), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Client introuvable.' : 'Impossible de charger ce client. Réessayez.');
        return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected submit() {
    if (!this.ready() || this.saving()) return;
    // Revalidate before retrying: server errors must not permanently lock the form.
    Object.values(this.form.controls).forEach(control => control.updateValueAndValidity());
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    const request: CustomerRequest = { name: value.name.trim(), company: value.company.trim() || null,
      email: value.email.trim() || null, phone: value.phone.trim() || null, address: value.address.trim() || null,
      contactType: value.contactType, category: value.category.trim() || null, segment: value.segment.trim() || null };
    this.saving.set(true); this.error.set('');
    const id = this.id();
    (id === null ? this.service.create(request) : this.service.update(id, request)).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.snack.open(id === null ? 'Client créé.' : 'Client modifié.', 'Fermer', { duration: 5000 });
        void this.router.navigate(['/customers']);
      },
      error: (e: { status?: number; error?: { message?: unknown } }) => {
        if (e.status === 401 || e.status === 403) return;
        if (e.status === 404) { this.ready.set(false); this.error.set('Ce client n’existe plus.'); return; }
        if (e.status === 400 || e.status === 409) {
          const message = typeof e.error?.message === 'string' ? e.error.message : '';
          // Interpret only known backend validation/duplicate fields; never reflect raw SQL or HTML.
          let fieldError = false;
          for (const key of ['name', 'email', 'phone', 'company', 'address'] as const) {
            if (message.includes(key + ':') || new RegExp('Customer ' + key + ' already exists', 'i').test(message)) {
              this.form.controls[key].setErrors({ ...this.form.controls[key].errors, server: true });
              this.form.controls[key].markAsTouched(); fieldError = true;
            }
          }
          this.error.set(fieldError ? 'Enregistrement refusé. Vérifiez les champs signalés.'
            : 'Enregistrement refusé. Vérifiez les informations et les doublons de nom, e-mail ou téléphone.');
        } else this.error.set('Impossible d’enregistrer le client. Réessayez plus tard.');
      }
    });
  }
}
