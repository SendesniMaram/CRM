import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { LeaveService } from './leave.service';

export function localToday(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
export function leaveDates(control: AbstractControl) {
  const { startDate, endDate } = control.value;
  const valid = (value: string) => /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value)) && new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;
  if ((startDate && !valid(startDate)) || (endDate && !valid(endDate))) return { invalidDate: true };
  if ((startDate && startDate < localToday()) || (endDate && endDate < localToday())) return { pastDate: true };
  return startDate && endDate && endDate < startDate ? { dateRange: true } : null;
}
@Component({
  selector: 'app-leave-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './leave-form.component.html', changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaveFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(LeaveService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly id = signal<number | null>(null);
  protected readonly ready = signal(false);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = this.fb.nonNullable.group({ startDate: ['', Validators.required], endDate: ['', Validators.required], reason: ['', Validators.maxLength(500)] }, { validators: leaveDates });
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.ready.set(false); this.error.set(''); this.form.reset(); this.id.set(null);
      const raw = params.get('id');
      if (raw === null) { this.ready.set(true); return EMPTY; }
      const id = Number(raw);
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Demande introuvable.'); return EMPTY; }
      this.id.set(id); this.loading.set(true);
      return this.service.get(id).pipe(tap(row => {
        if (row.status !== 'PENDING') { this.error.set('Une demande déjà décidée ne peut plus être modifiée.'); return; }
        this.form.patchValue({ startDate: row.startDate, endDate: row.endDate, reason: row.reason || '' }); this.ready.set(true);
      }), catchError(() => { this.error.set('Demande introuvable, inaccessible ou service indisponible.'); return EMPTY; }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroyRef)).subscribe();
  }
  protected submit() {
    if (!this.ready() || this.saving()) return;
    this.form.updateValueAndValidity();
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    const request = { startDate: value.startDate, endDate: value.endDate, reason: value.reason.trim() || null };
    this.saving.set(true); this.error.set('');
    const id = this.id();
    (id === null ? this.service.create(request) : this.service.update(id, request)).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => void this.router.navigate(['/leaves']),
      error: (e: { status?: number }) => this.error.set(e.status === 403
        ? 'Accès refusé. Vérifiez votre association Employee avec un administrateur.'
        : e.status === 400 || e.status === 409 ? 'Enregistrement refusé. Vérifiez les dates ou rechargez la demande.' : 'Impossible d’enregistrer la demande. Réessayez.')
    });
  }
}
