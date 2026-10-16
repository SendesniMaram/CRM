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
import { FeeReferencePickerComponent } from '../fees/fee-reference-picker.component';
import { PayrollRequest } from './payroll.models';
import { PayrollService } from './payroll.service';

const notBlank = (control: AbstractControl) => typeof control.value === 'string' && control.value.trim() ? null : { required: true };
const finite = (control: AbstractControl) => control.value === null || Number.isFinite(control.value) ? null : { number: true };
const positive = (control: AbstractControl) => control.value === null || control.value > 0 ? null : { positive: true };
export function payrollDate(control: AbstractControl) {
  const value: string = control.value;
  if (!value) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return { date: true };
  const date = new Date(value + 'T00:00:00Z');
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value ? null : { date: true };
}

@Component({
  selector: 'app-payroll-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule, FeeReferencePickerComponent],
  templateUrl: './payroll-form.component.html', styleUrl: './payroll.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PayrollFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PayrollService);
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
  protected readonly lookup = signal(false);
  protected readonly textFields = [
    { key: 'payPeriod', label: 'Période de paie', max: 50, required: true },
    { key: 'currency', label: 'Devise', max: 10, required: false },
    { key: 'status', label: 'Statut', max: 50, required: true }
  ] as const;
  protected readonly amountFields = [
    { key: 'baseSalary', label: 'Salaire de base', required: true },
    { key: 'bonuses', label: 'Primes', required: false },
    { key: 'deductions', label: 'Retenues', required: false },
    { key: 'taxAmount', label: 'Impôt', required: false }
  ] as const;
  protected readonly form = this.fb.group({
    employeeId: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)]),
    baseSalary: this.fb.control<number | null>(null, [Validators.required, finite, positive]),
    bonuses: this.fb.control<number | null>(null, finite),
    deductions: this.fb.control<number | null>(null, finite),
    taxAmount: this.fb.control<number | null>(null, finite),
    payPeriod: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)]),
    paymentDate: this.fb.nonNullable.control('', payrollDate),
    currency: this.fb.nonNullable.control('', Validators.maxLength(10)),
    status: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(50)])
  });

  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.ready.set(false); this.error.set(''); this.form.reset(); this.lookup.set(false);
      this.id.set(null); this.loading.set(false);
      const raw = params.get('id');
      if (raw === null) { this.ready.set(true); return EMPTY; }
      const id = Number(raw);
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Fiche de paie introuvable.'); return EMPTY; }
      this.id.set(id); this.loading.set(true);
      return this.service.get(id).pipe(tap(row => {
        this.form.patchValue({ employeeId: row.employeeId, baseSalary: row.baseSalary,
          bonuses: row.bonuses, deductions: row.deductions, taxAmount: row.taxAmount,
          payPeriod: row.payPeriod, paymentDate: row.paymentDate ?? '',
          currency: row.currency ?? '', status: row.status });
        this.ready.set(true);
      }), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Fiche de paie introuvable.' : 'Impossible de charger la fiche de paie. Réessayez.');
        return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected canManage() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  protected selectEmployee(value: string) {
    if (this.saving() || !this.canManage()) return;
    this.form.controls.employeeId.setValue(value); this.lookup.set(false);
  }
  protected submit() {
    if (!this.ready() || this.loading() || this.saving() || !this.canManage()) return;
    Object.values(this.form.controls).forEach(control => control.updateValueAndValidity());
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    if (value.baseSalary === null) return;
    const request: PayrollRequest = { employeeId: value.employeeId.trim(), baseSalary: value.baseSalary,
      bonuses: value.bonuses, deductions: value.deductions, taxAmount: value.taxAmount,
      payPeriod: value.payPeriod.trim(), paymentDate: value.paymentDate || null,
      currency: value.currency.trim() || null, status: value.status.trim() };
    this.saving.set(true); this.error.set('');
    const id = this.id();
    (id === null ? this.service.create(request) : this.service.update(id, request)).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.snack.open(id === null ? 'Fiche de paie créée.' : 'Fiche de paie modifiée.', 'Fermer', { duration: 5000 });
        void this.router.navigate(['/payroll']);
      },
      error: (e: { status?: number; error?: { message?: unknown } }) => {
        if (e.status === 401 || e.status === 403) return;
        if (e.status === 404) { this.ready.set(false); this.error.set('Cette fiche de paie n’existe plus.'); return; }
        if (e.status === 400 || e.status === 409) {
          const message = typeof e.error?.message === 'string' ? e.error.message : '';
          for (const key of Object.keys(this.form.controls) as (keyof typeof this.form.controls)[]) {
            if (message.includes(key + ':')) {
              this.form.controls[key].setErrors({ server: true }); this.form.controls[key].markAsTouched();
            }
          }
          this.error.set('Enregistrement refusé. Vérifiez les champs signalés et les montants saisis.');
        } else this.error.set('Impossible d’enregistrer la fiche de paie. Réessayez plus tard.');
      }
    });
  }
}
