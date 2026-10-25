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
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { FeeReferencePickerComponent } from './fee-reference-picker.component';
import { FeeRequest } from './fee.models';
import { FeeService } from './fee.service';

const notBlank = (control: AbstractControl) => typeof control.value === 'string' && control.value.trim() ? null : { required: true };
const positiveFinite = (control: AbstractControl) => control.value === null
  || (Number.isFinite(control.value) && control.value > 0) ? null : { positive: true };
export function feeDate(control: AbstractControl) {
  const value: string = control.value;
  if (!value) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return { date: true };
  const date = new Date(value + 'T00:00:00Z');
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value ? null : { date: true };
}

@Component({
  selector: 'app-fee-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule, FeeReferencePickerComponent],
  templateUrl: './fee-form.component.html', styleUrl: './fees.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(FeeService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy = inject(DestroyRef);
  protected readonly id = signal<number | null>(null);
  protected readonly ready = signal(false);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly lookup = signal<'employee' | 'customer' | null>(null);
  protected readonly textFields = [
    { key: 'serviceName', label: 'Prestation', max: 255, required: true },
    { key: 'invoiceStatus', label: 'Statut de facturation', max: 50, required: false },
    { key: 'paymentStatus', label: 'Statut de paiement', max: 50, required: false }
  ] as const;
  protected readonly form = this.fb.group({
    employeeId: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)]),
    customerId: this.fb.nonNullable.control('', Validators.maxLength(50)),
    serviceName: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(255)]),
    description: this.fb.nonNullable.control('', Validators.maxLength(500)),
    hourlyRate: this.fb.control<number | null>(null, [Validators.required, positiveFinite]),
    workedHours: this.fb.control<number | null>(null, [Validators.required, positiveFinite]),
    invoiceStatus: this.fb.nonNullable.control('', Validators.maxLength(50)),
    paymentStatus: this.fb.nonNullable.control('', Validators.maxLength(50)),
    workDate: this.fb.nonNullable.control('', feeDate)
  });

  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.ready.set(false); this.error.set(''); this.form.reset(); this.lookup.set(null);
      this.id.set(null); this.loading.set(false);
      const raw = params.get('id');
      if (raw === null) { this.ready.set(true); return EMPTY; }
      const id = Number(raw);
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Honoraire introuvable.'); return EMPTY; }
      this.id.set(id); this.loading.set(true);
      return this.service.get(id).pipe(tap(row => {
        this.form.patchValue({ employeeId: row.employeeId, customerId: row.customerId ?? '',
          serviceName: row.serviceName, description: row.description ?? '', hourlyRate: row.hourlyRate,
          workedHours: row.workedHours, invoiceStatus: row.invoiceStatus ?? '',
          paymentStatus: row.paymentStatus ?? '', workDate: row.workDate ?? '' });
        this.ready.set(true);
      }), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Honoraire introuvable.' : 'Impossible de charger l’honoraire. Réessayez.');
        return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected canManage() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected selectReference(kind: 'employee' | 'customer', value: string) {
    if (this.saving() || !this.canManage()) return;
    this.form.controls[kind === 'employee' ? 'employeeId' : 'customerId'].setValue(value);
    this.lookup.set(null);
  }
  protected submit() {
    if (!this.ready() || this.loading() || this.saving() || !this.canManage()) return;
    Object.values(this.form.controls).forEach(control => control.updateValueAndValidity());
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    if (value.hourlyRate === null || value.workedHours === null) return;
    const request: FeeRequest = { employeeId: value.employeeId.trim(), customerId: value.customerId.trim() || null,
      serviceName: value.serviceName.trim(), description: value.description.trim() || null,
      hourlyRate: value.hourlyRate, workedHours: value.workedHours,
      invoiceStatus: value.invoiceStatus.trim() || null, paymentStatus: value.paymentStatus.trim() || null,
      workDate: value.workDate || null };
    this.saving.set(true); this.error.set('');
    const id = this.id();
    (id === null ? this.service.create(request) : this.service.update(id, request)).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.snack.open(id === null ? 'Honoraire créé.' : 'Honoraire modifié.', 'Fermer', { duration: 5000 });
        void this.router.navigate(['/fees']);
      },
      error: (e: { status?: number; error?: { message?: unknown } }) => {
        if (e.status === 401 || e.status === 403) return;
        if (e.status === 404) { this.ready.set(false); this.error.set('Cet honoraire n’existe plus.'); return; }
        if (e.status === 400 || e.status === 409) {
          const message = typeof e.error?.message === 'string' ? e.error.message : '';
          for (const key of Object.keys(this.form.controls) as (keyof typeof this.form.controls)[]) {
            if (message.includes(key + ':')) {
              this.form.controls[key].setErrors({ server: true }); this.form.controls[key].markAsTouched();
            }
          }
          this.error.set('Enregistrement refusé. Vérifiez les champs signalés et les informations saisies.');
        } else this.error.set('Impossible d’enregistrer l’honoraire. Réessayez plus tard.');
      }
    });
  }
}
