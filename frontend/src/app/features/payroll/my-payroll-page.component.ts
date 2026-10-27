import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { finalize } from 'rxjs';
import { CrmRole } from '../../core/models/role.model';
import { AuthService } from '../../core/services/auth.service';
import { PayrollResponse } from './payroll.models';
import { MyPayrollService } from './my-payroll.service';

@Component({
  selector: 'app-my-payroll-page', standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatPaginatorModule, MatTableModule],
  templateUrl: './my-payroll-page.component.html', styleUrl: './payroll.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyPayrollPageComponent implements OnInit {
  private readonly service = inject(MyPayrollService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  protected readonly period = new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^$|^\d{4}(-\d{2})?$/)] });
  protected readonly rows = signal<PayrollResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly page = signal(0);
  protected readonly total = signal(0);
  protected readonly pages = signal(0);
  ngOnInit() { this.load(); }

  protected load(page = 0) {
    if (!this.auth.hasAnyRole([CrmRole.EMPLOYEE]) || this.loading()) return;
    if (this.period.invalid) { this.period.markAsTouched(); return; }
    const generation = this.auth.sessionGeneration;
    this.loading.set(true); this.error.set('');
    this.service.list(page, this.period.value.trim()).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => { if (generation === this.auth.sessionGeneration && !this.destroy.destroyed) this.loading.set(false); })
    ).subscribe({
      next: result => {
        if (generation !== this.auth.sessionGeneration || this.destroy.destroyed) return;
        this.rows.set(result.content); this.page.set(result.number); this.total.set(result.totalElements); this.pages.set(result.totalPages);
      },
      error: (failure: { status?: number }) => {
        if (generation !== this.auth.sessionGeneration || this.destroy.destroyed) return;
        this.rows.set([]); this.error.set(payrollError(failure.status));
      }
    });
  }

  protected filter() { this.load(0); }
  protected paginate(event: PageEvent) { this.load(event.pageIndex); }
  protected readonly columns = ['payPeriod', 'paymentDate', 'netSalary', 'currency', 'status', 'actions'];
}

export function payrollError(status?: number): string {
  return ({ 400: 'Période invalide. Utilisez AAAA ou AAAA-MM.', 401: 'Session expirée. Reconnectez-vous.',
    403: 'Accès réservé au compte employé associé.', 404: 'Aucun bulletin ou aucune association employé trouvée.',
    502: 'Réponse du service invalide.', 503: 'Service de paie ou association temporairement indisponible.'
  } as Record<number, string>)[status ?? 0] ?? 'Impossible de charger vos bulletins. Réessayez.';
}
