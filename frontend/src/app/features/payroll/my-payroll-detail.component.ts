import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize, switchMap } from 'rxjs';
import { MyPayrollService } from './my-payroll.service';
import { PayrollResponse } from './payroll.models';
import { payrollError } from './my-payroll-page.component';

@Component({
  selector: 'app-my-payroll-detail', standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatCardModule],
  templateUrl: './my-payroll-detail.component.html', styleUrl: './payroll.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyPayrollDetailComponent implements OnInit {
  private readonly service = inject(MyPayrollService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  protected readonly row = signal<PayrollResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly downloading = signal(false);
  protected readonly error = signal('');

  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      const id = params.get('id') ?? '';
      this.loading.set(true); this.error.set(''); this.row.set(null);
      return this.service.read(id).pipe(finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe({
      next: row => this.row.set(row),
      error: failure => { this.error.set(payrollError(failure.status)); }
    });
  }

  protected download() {
    const row = this.row();
    if (!row || this.downloading()) return;
    this.downloading.set(true); this.error.set('');
    this.service.pdf(String(row.id)).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.downloading.set(false))).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob); const anchor = document.createElement('a');
        anchor.href = url; anchor.download = `bulletin-${row.payPeriod}.pdf`; anchor.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: failure => this.error.set(payrollError(failure.status))
    });
  }
}
