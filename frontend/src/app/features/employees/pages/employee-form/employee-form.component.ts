import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { EmployeeRequest } from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';

const notBlank: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.trim() ? null : { required: true };

const positiveInteger: ValidatorFn = (control) =>
  control.value == null || (Number.isSafeInteger(control.value) && control.value > 0)
    ? null : { positiveInteger: true };

const finiteNumber: ValidatorFn = (control) =>
  control.value == null || Number.isFinite(control.value) ? null : { number: true };

const pastOrPresent: ValidatorFn = (control) => {
  if (!control.value) return null;
  const today = new Date();
  const date = new Date(`${control.value}T00:00:00`);
  today.setHours(0, 0, 0, 0);
  return Number.isNaN(date.getTime()) || date > today ? { futureDate: true } : null;
};

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EmployeeService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly employeeId = signal<number | null>(null);
  protected readonly editing = signal(false);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly ready = signal(false);
  protected readonly notFound = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly textFields = [
    { name: 'employeeCode', label: 'Code employé', max: 255 },
    { name: 'firstName', label: 'Prénom', max: 255 },
    { name: 'lastName', label: 'Nom', max: 255 },
    { name: 'email', label: 'E-mail', max: 255 },
    { name: 'phone', label: 'Téléphone', max: 50 },
    { name: 'jobTitle', label: 'Poste', max: 255 },
    { name: 'gender', label: 'Genre', max: 50 },
    { name: 'status', label: 'Statut', max: 50 }
  ] as const;
  protected readonly form = this.fb.group({
    employeeCode: this.fb.nonNullable.control('', [Validators.required, notBlank, Validators.maxLength(255)]),
    firstName: this.fb.nonNullable.control('', Validators.maxLength(255)),
    lastName: this.fb.nonNullable.control('', Validators.maxLength(255)),
    email: this.fb.nonNullable.control('', [Validators.maxLength(255),
      (control: AbstractControl) => Validators.email({ value: control.value.trim() } as AbstractControl)]),
    phone: this.fb.nonNullable.control('', Validators.maxLength(50)),
    jobTitle: this.fb.nonNullable.control('', Validators.maxLength(255)),
    hireDate: this.fb.nonNullable.control('', pastOrPresent),
    salary: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), finiteNumber]),
    address: this.fb.nonNullable.control('', Validators.maxLength(500)),
    dateOfBirth: this.fb.nonNullable.control(''),
    gender: this.fb.nonNullable.control('', Validators.maxLength(50)),
    status: this.fb.nonNullable.control('', Validators.maxLength(50)),
    departmentId: this.fb.control<number | null>(null, positiveInteger)
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap((params) => {
        this.form.reset();
        this.ready.set(false);
        this.notFound.set(false);
        this.errorMessage.set('');
        const rawId = params.get('id');
        this.editing.set(rawId !== null);
        this.employeeId.set(null);
        if (rawId === null) {
          this.ready.set(true);
          return EMPTY;
        }
        const id = Number(rawId);
        if (!Number.isSafeInteger(id) || id <= 0) {
          this.notFound.set(true);
          return EMPTY;
        }
        this.employeeId.set(id);
        this.loading.set(true);
        return this.service.getEmployeeById(id).pipe(
          tap((employee) => {
            this.form.patchValue({
              employeeCode: employee.employeeCode,
              firstName: employee.firstName ?? '', lastName: employee.lastName ?? '',
              email: employee.email ?? '', phone: employee.phone ?? '',
              jobTitle: employee.jobTitle ?? '', hireDate: employee.hireDate ?? '',
              salary: employee.salary, address: employee.address ?? '',
              dateOfBirth: employee.dateOfBirth ?? '', gender: employee.gender ?? '',
              status: employee.status ?? '', departmentId: employee.departmentId ?? null
            });
            this.ready.set(true);
          }),
          catchError((error: { status?: number }) => {
            if (error.status === 404) this.notFound.set(true);
            else this.errorMessage.set('Impossible de charger l’employé. Réessayez plus tard.');
            return EMPTY;
          }),
          finalize(() => this.loading.set(false))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  protected submit(): void {
    if (this.saving() || !this.ready() || this.notFound()) return;
    this.form.controls.hireDate.updateValueAndValidity();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const optional = (text: string): string | undefined => text.trim() || undefined;
    const payload: EmployeeRequest = {
      employeeCode: this.editing() ? value.employeeCode : value.employeeCode.trim(), salary: Number(value.salary),
      firstName: optional(value.firstName), lastName: optional(value.lastName),
      email: optional(value.email), phone: optional(value.phone),
      jobTitle: optional(value.jobTitle), hireDate: optional(value.hireDate),
      address: optional(value.address), dateOfBirth: optional(value.dateOfBirth),
      gender: optional(value.gender), status: optional(value.status),
      departmentId: value.departmentId == null ? undefined : Number(value.departmentId)
    };
    const id = this.employeeId();
    this.saving.set(true);
    this.errorMessage.set('');
    const request = id === null ? this.service.createEmployee(payload) : this.service.updateEmployee(id, payload);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: (employee) => void this.router.navigate(['/employees', employee.id]),
      error: (error: { status?: number }) => {
        if (id !== null && error.status === 404) {
          this.notFound.set(true);
          return;
        }
        this.errorMessage.set(error.status === 400 || error.status === 409
          ? 'Enregistrement refusé. Vérifiez les informations, notamment l’unicité du code employé et de l’e-mail.'
          : 'Impossible d’enregistrer l’employé. Réessayez plus tard.');
      }
    });
  }
}
