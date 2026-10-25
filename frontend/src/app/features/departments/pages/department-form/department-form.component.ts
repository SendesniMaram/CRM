import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { DepartmentRequest, DepartmentService } from '../../services/department.service';

@Component({
  selector: 'app-department-form', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './department-form.component.html', styleUrl: './department-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DepartmentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DepartmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly departmentId = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly ready = signal(false);
  protected readonly notFound = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255), (control: AbstractControl) => control.value.trim() ? null : { required: true }]],
    code: ['', Validators.maxLength(50)],
    description: ['', Validators.maxLength(255)]
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap((params) => {
        this.form.reset(); this.ready.set(false); this.notFound.set(false); this.errorMessage.set('');
        this.departmentId.set(null);
        const raw = params.get('id');
        if (raw === null) { this.ready.set(true); return EMPTY; }
        const id = Number(raw);
        if (!Number.isSafeInteger(id) || id <= 0) { this.notFound.set(true); return EMPTY; }
        this.departmentId.set(id); this.loading.set(true);
        return this.service.getDepartmentById(id).pipe(
          tap((department) => {
            this.form.patchValue({ name: department.name, code: department.code ?? '', description: department.description ?? '' });
            this.ready.set(true);
          }),
          catchError((error: { status?: number }) => {
            if (error.status === 404) this.notFound.set(true);
            else this.errorMessage.set('Impossible de charger le département. Réessayez.');
            return EMPTY;
          }), finalize(() => this.loading.set(false))
        );
      }), takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  protected submit(): void {
    if (!this.ready() || this.saving() || this.notFound()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    const request: DepartmentRequest = { name: value.name.trim(), code: value.code.trim() || null, description: value.description.trim() || null };
    this.saving.set(true); this.errorMessage.set('');
    const id = this.departmentId();
    const result = id === null ? this.service.createDepartment(request) : this.service.updateDepartment(id, request);
    result.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => void this.router.navigate(['/departments']),
      error: (error: { status?: number }) => {
        if (error.status === 404 && id !== null) { this.notFound.set(true); return; }
        this.errorMessage.set(error.status === 400 || error.status === 409
          ? 'Enregistrement refusé. Vérifiez les champs et l’unicité du nom et du code.'
          : 'Impossible d’enregistrer le département. Réessayez plus tard.');
      }
    });
  }
}
